/*
 * Copyright 2011 <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ocpsoft.rewrite.servlet.config;

import java.util.Map;

import org.ocpsoft.common.util.Assert;
import org.ocpsoft.rewrite.bind.Binding;
import org.ocpsoft.rewrite.bind.Bindings;
import org.ocpsoft.rewrite.bind.Evaluation;
import org.ocpsoft.rewrite.bind.ParameterizedPatternImpl;
import org.ocpsoft.rewrite.context.EvaluationContext;
import org.ocpsoft.rewrite.param.ParameterStore;
import org.ocpsoft.rewrite.param.PatternParameter;
import org.ocpsoft.rewrite.servlet.config.bind.Request;
import org.ocpsoft.rewrite.servlet.http.event.HttpOutboundServletRewrite;
import org.ocpsoft.rewrite.servlet.http.event.HttpServletRewrite;

/**
 * A {@link org.ocpsoft.rewrite.config.Condition} that inspects the value of
 * {@link org.ocpsoft.rewrite.servlet.http.event.HttpServletRewrite#getRequestPath()}
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class Path extends HttpCondition implements IPath
{
   private final ParameterizedPatternImpl expression;
   private final ParameterStore<PathParameter> parameters = new ParameterStore<PathParameter>();

   private Path(final String pattern)
   {
      Assert.notNull(pattern, "Path must not be null.");
      this.expression = new ParameterizedPatternImpl("[^/]+", pattern);

      for (PatternParameter parameter : this.expression.getParameterMap().values()) {
         where(parameter.getName()).bindsTo(Evaluation.property(parameter.getName()));
      }
   }

   /**
    * Inspect the current request URL, comparing against the given pattern.
    * <p>
    * The given pattern may be parameterized using the following format:
    * <p>
    * <code>
    *    /example/{param} <br>
    *    /example/{value}/sub/{value2} <br>
    *    ... and so on
    * </code>
    * <p>
    * By default, matching parameter values are bound only to the {@link org.ocpsoft.rewrite.context.EvaluationContext}.
    * See also {@link #where(String)}
    */
   public static IPath matches(final String pattern)
   {
      return new Path(pattern);
   }

   public static IPath captureIn(final String param)
   {
      Path path = new Path("{" + param + "}");
      path.where(param).matches(".*");
      return path;
   }

   /**
    * Bind each path parameter to the corresponding request parameter by name. By default, matching values are bound
    * only to the {@link org.ocpsoft.rewrite.context.EvaluationContext}.
    * <p>
    * See also {@link #where(String)}
    */
   @Override
   public IPath withRequestBinding()
   {
      for (PatternParameter capture : expression.getParameterMap().values()) {
         where(capture.getName()).bindsTo(Request.parameter(capture.getName()));
      }
      return this;
   }

   @Override
   public boolean evaluateHttp(final HttpServletRewrite event, final EvaluationContext context)
   {
      String requestURL = null;

      if (event instanceof HttpOutboundServletRewrite)
      {
         requestURL = ((HttpOutboundServletRewrite) event).getOutboundURL().split("\\?")[0];
         if (requestURL.startsWith(event.getContextPath()))
         {
            requestURL = requestURL.substring(event.getContextPath().length());
         }
      }
      else
         requestURL = event.getRequestPath();

      if (expression.matches(event, context, requestURL))
      {
         Map<PatternParameter, String[]> parameters = expression.parse(event, context, requestURL);

         for (PatternParameter capture : parameters.keySet()) {
            if (!Bindings.enqueueSubmission(event, context, where(capture.getName()), parameters.get(capture)))
               return false;
         }
         return true;
      }
      return false;
   }

   /**
    * Get the underlying {@link ParameterizedPatternImpl} for this {@link Path}
    * <p>
    * See also: {@link #where(String)}
    */
   @Override
   public ParameterizedPatternImpl getPathExpression()
   {
      return expression;
   }

   @Override
   public String toString()
   {
      return expression.toString();
   }

   @Override
   public PathParameter where(String param)
   {
      return parameters.where(param, new PathParameter(this, expression.getParameter(param)));
   }

   @Override
   public PathParameter where(String param, Binding binding)
   {
      return where(param).bindsTo(binding);
   }
}
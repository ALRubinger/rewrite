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
package org.ocpsoft.rewrite.servlet.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ocpsoft.common.services.NonEnriching;
import org.ocpsoft.common.services.ServiceLoader;
import org.ocpsoft.common.util.Iterators;
import org.ocpsoft.rewrite.servlet.http.HttpRequestCycleWrapper;
import org.ocpsoft.rewrite.servlet.spi.RequestParameterProvider;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 *
 */
public class HttpRewriteRequestCycleWrapper extends HttpRequestCycleWrapper implements NonEnriching
{
   private volatile List<RequestParameterProvider> providers;

   @SuppressWarnings("unchecked")
   public HttpRewriteRequestCycleWrapper()
   {
      if (providers == null)
         synchronized (this) {
            if (providers == null)
               providers = Iterators.asList(ServiceLoader.load(RequestParameterProvider.class));
         }
   }

   @Override
   public HttpServletRequest wrapRequest(final HttpServletRequest request, final HttpServletResponse response)
   {
      Map<String, String[]> additionalParams = new HashMap<String, String[]>();

      for (RequestParameterProvider provider : providers) {
         Map<String, String[]> m = provider.getParameters(request, response);
         if (m != null)
         {
            additionalParams.putAll(m);
         }
      }

      HttpRewriteWrappedRequest wrapped = new HttpRewriteWrappedRequest(request, additionalParams);
      return wrapped;
   }

   @Override
   public HttpServletResponse wrapResponse(final HttpServletRequest request, final HttpServletResponse response)
   {
      return new HttpRewriteWrappedResponse(request, response);
   }

   @Override
   public int priority()
   {
      return 0;
   }
}

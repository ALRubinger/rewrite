/*
 * Copyright 2010 Lincoln Baxter, III
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
package org.ocpsoft.rewrite.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ocpsoft.common.pattern.WeightedComparator;
import org.ocpsoft.logging.Logger;
import org.ocpsoft.rewrite.annotation.api.ClassContext;
import org.ocpsoft.rewrite.annotation.api.ClassVisitor;
import org.ocpsoft.rewrite.annotation.context.ClassContextImpl;
import org.ocpsoft.rewrite.annotation.context.FieldContextImpl;
import org.ocpsoft.rewrite.annotation.context.MethodContextImpl;
import org.ocpsoft.rewrite.annotation.context.ParameterContextImpl;
import org.ocpsoft.rewrite.annotation.spi.AnnotationHandler;
import org.ocpsoft.rewrite.config.Configuration;
import org.ocpsoft.rewrite.config.ConfigurationBuilder;
import org.ocpsoft.rewrite.config.Rule;

public class ClassVisitorImpl implements ClassVisitor, Configuration
{

   private final Logger log = Logger.getLogger(ClassVisitorImpl.class);

   /**
    * list of handlers for processing annotations
    */
   private final List<AnnotationHandler<Annotation>> handlerList;

   /**
    * The rules created by the visitor
    */
   private final ConfigurationBuilder builder = ConfigurationBuilder.begin();

   /**
    * The visitor must be initialized with the handlers to call for specific annotations
    */
   public ClassVisitorImpl(List<AnnotationHandler<Annotation>> handlers)
   {
      handlerList = new ArrayList<AnnotationHandler<Annotation>>(handlers);
      Collections.sort(handlerList, new WeightedComparator());

      if (log.isDebugEnabled()) {
         log.debug("Initialized to use {} AnnotationHandlers..", handlers.size());
      }

   }

   /**
    * Processes the annotation on the supplied class.
    */
   @Override
   public void visit(Class<?> clazz)
   {

      ClassContext context = new ClassContextImpl(builder);

      if (log.isTraceEnabled()) {
         log.trace("Scanning class: {}", clazz.getName());
      }

      // first process the class
      visit(clazz, context);

      // then process the fields
      for (Field field : clazz.getDeclaredFields()) {
         visit(field, new FieldContextImpl(context));
      }

      // then the methods
      for (Method method : clazz.getDeclaredMethods()) {
         MethodContextImpl methodContext = new MethodContextImpl(context);
         visit(method, methodContext);

         // then the method parameters
         for (int i = 0; i < method.getParameterTypes().length; i++) {
            visit(new ParameterImpl(method, method.getParameterTypes()[i], method.getParameterAnnotations()[i], i),
                     new ParameterContextImpl(methodContext));
         }
      }

   }

   /**
    * Process one {@link AnnotatedElement} of the class.
    */
   private void visit(AnnotatedElement element, ClassContext context)
   {

      // each annotation on the element may be interesting for us
      for (Annotation annotation : element.getAnnotations()) {

         // check if any of the handlers is responsible
         for (AnnotationHandler<Annotation> handler : handlerList) {
            if (handler.handles().equals(annotation.annotationType())) {
               handler.process(context, element, annotation);
            }
         }

      }
   }

   @Override
   public List<Rule> getRules()
   {
      return builder.getRules();
   }

}

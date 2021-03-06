/* Copyright 2010 Abhinav Sarkar <abhinav@abhinavsarkar.net>
 *
 * This file is a part of SpelHelper library.
 *
 * SpelHelper library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License (GNU LGPL) as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * SpelHelper library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with SpelHelper library.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.abhinavsarkar.spelhelper;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypedValue;
import org.springframework.util.Assert;

final class ImplicitPropertyAccessor extends ReadOnlyGenericPropertyAccessor {

    private static final ConcurrentHashMap<String, MethodExecutor> CACHE =
        new ConcurrentHashMap<String, MethodExecutor>();

    private static final MethodExecutor NULL_ME = new MethodExecutor() {
        @Override
        public TypedValue execute(final EvaluationContext context, final Object target,
                final Object... arguments) throws AccessException {
            throw new UnsupportedOperationException("This method should never be called");
        }
    };

    @Override
    public boolean canRead(final EvaluationContext context,
            final Object target, final String name)
            throws AccessException {
        Assert.notNull(target, "target is null");
        String cacheKey = target.getClass().getName() + "." + name;
        if (CACHE.containsKey(cacheKey)) {
            return CACHE.get(cacheKey) != NULL_ME;
        }

        for (MethodResolver mr : context.getMethodResolvers()) {
            MethodExecutor me =
                mr.resolve(context, target, name, Collections.<TypeDescriptor>emptyList());
            if (me != null) {
                CACHE.putIfAbsent(cacheKey, me);
                return true;
            }
        }

        CACHE.putIfAbsent(cacheKey, NULL_ME);
        return false;
    }

    @Override
    public TypedValue read(final EvaluationContext context,
            final Object target, final String name)
            throws AccessException {
        if (canRead(context, target, name)) {
            String cacheKey = target.getClass().getName() + "." + name;
            return CACHE.get(cacheKey).execute(context, target, new Object[0]);
        }
        throw new AccessException(MessageFormat.format(
                "Cannot read property: {0} of target: {1}", name, target));
    }

}
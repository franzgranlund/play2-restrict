/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Franz Granlund
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package restrict;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import play.Logger;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import java.util.List;
import static play.Play.application;

public class RestrictToHostGroupAction extends Action<RestrictToHostGroup> {
    private static final String RESTRICT_ALREADY_SET = "restricttohostgroup-set";
    private static final String CONFIG_KEY_DEFAULT = "default";
    private static final String CONFIG_KEY_GROUPS = "restricttohostgroup.groups.";
    private static final String CONFIG_KEY_DENIED_REDIRECT = "restricttohostgroup.redirect";
    private static final String MSG_GROUP_NOT_FOUND = "RestrictToHostGroup - Group '%s' not found. Using 'default'.";
    private static final String MSG_LOG_ACCESS_DENIED = "RestrictToHostGroup - Access denied for %s [%s]";

    private static final String REDIRECT_URL = application().configuration().getString(CONFIG_KEY_DENIED_REDIRECT);

    @Override
    public F.Promise<Result> call(Http.Context ctx) throws Throwable {
        if (!ctx.args.containsKey(RESTRICT_ALREADY_SET)) {
            ctx.args.put(RESTRICT_ALREADY_SET, "");

            String value = configuration.value();
            String group = (value == null || value.isEmpty()) ? CONFIG_KEY_DEFAULT : value;
            String remoteAddress = trimAddress(ctx.request().remoteAddress());

            /**
             * Check if our application.conf contains the group requested. Otherwise
             * fall back to default.
             */
            if (!application().configuration().keys().contains(CONFIG_KEY_GROUPS + group)) {
                Logger.warn(String.format(MSG_GROUP_NOT_FOUND, group));
                group = CONFIG_KEY_DEFAULT;
            }

            /**
             * Loop through the access list for the group. Return on first match.
             */
            List<String> allowedList = application().configuration().getStringList(CONFIG_KEY_GROUPS + group);
            if (allowedList.stream().anyMatch(p -> matchesPattern(remoteAddress, p))) {
                return delegate.call(ctx);
            }

            /**
             * Log a warning if some unauthorized IP requests access.
             */
            Logger.warn(String.format(MSG_LOG_ACCESS_DENIED, remoteAddress, ctx.request().uri()));

            /**
             * Redirect the user if we've configured a redirect key in application.conf.
             */
            if (REDIRECT_URL != null) {
                return F.Promise.pure(redirect(REDIRECT_URL));
            }

            return F.Promise.pure(forbidden());
        }

        return delegate.call(ctx);
    }

    private Boolean matchesPattern(String remoteAddress, String pattern) {
        if (pattern.contains("/") && !isIPv6(remoteAddress)) {
            /**
             * Seems like we have a CIDR
             */
            SubnetUtils su = new SubnetUtils(pattern);
            //su.setInclusiveHostCount(true);
            return su.getInfo().isInRange(remoteAddress);
        }

        return StringUtils.equals(remoteAddress, pattern);
    }

    private String trimAddress(String addr) {
        /**
         * IPv6 addresses might contain a % at the end. Return a clean IPv6.
         */
        if (isIPv6(addr)) {
            return StringUtils.substringBefore(addr, "%");
        }
        return addr;
    }

    private Boolean isIPv6(String addr) {
        return StringUtils.contains(addr, ":");
    }
}

/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datameer.awstasks.util;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InstanceStateName;

public class Filters {

    public static Filter groupName(String... values) {
        return new Filter("group-name").withValues(values);
    }

    public static Filter reservationId(String value) {
        return new Filter("reservation-id").withValues(value);
    }

    public static Filter instanceStates(InstanceStateName... values) {
        return new Filter("instance-state-name").withValues(Ec2Util.toStrings(values));
    }

    public static Filter permissionProtocol(String... values) {
        return new Filter("ip-permission.protocol").withValues(values);
    }

}

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
package datameer.awstasks.aws;

/**
 * Link to description <a href="http://aws.amazon.com/ec2/#instance">instance description</a>.
 * 
 */
public enum InstanceType {

    SMALL("m1.small"), LARGE("m1.large"), EXTRA_LARGE("m1.xlarge"), MEMORY_DOUBLE_XLARGE("m2.2xlarge"), MEMORY_QUAD_XLARGE("m2.4xlarge"), CPU_MEDIUM("c1.medium"), CPU_XLARGE("c1.xlarge");

    private final String _id;

    private InstanceType(String id) {
        _id = id;
    }

    public String getId() {
        return _id;
    }

    public String getFullName() {
        return name() + "(" + getId() + ")";
    }

}

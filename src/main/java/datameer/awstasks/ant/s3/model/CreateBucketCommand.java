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
package datameer.awstasks.ant.s3.model;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;

public class CreateBucketCommand extends S3Command {

    private String _name;
    private String _location;
    private boolean _deleteBefore;

    public void setName(String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public void setLocation(String location) {
        _location = location;
    }

    public String getLocation() {
        return _location;
    }

    public void setDeleteBefore(boolean deleteBefore) {
        _deleteBefore = deleteBefore;
    }

    public boolean isDeleteBefore() {
        return _deleteBefore;
    }

    @Override
    public void execute(S3Service s3Service) throws S3ServiceException {
        // S3Bucket s3Bucket = new S3Bucket(_name, _location);
        if (isDeleteBefore() && s3Service.getBucket(_name) != null) {
            s3Service.deleteBucket(_name);
        }
        s3Service.getOrCreateBucket(_name, _location);
    }

}

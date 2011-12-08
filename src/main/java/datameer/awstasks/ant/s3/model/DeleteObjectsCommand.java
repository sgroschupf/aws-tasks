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

import java.util.List;

import org.apache.tools.ant.Project;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class DeleteObjectsCommand extends S3Command {

    private String _bucket;
    private String _prefix;

    public String getBucket() {
        return _bucket;
    }

    public void setBucket(String bucket) {
        _bucket = bucket;
    }

    public String getPrefix() {
        return _prefix;
    }

    public void setPrefix(String prefix) {
        _prefix = prefix;
    }

    @Override
    public void execute(Project project, AmazonS3 s3Service) {
        if (_prefix.startsWith("/")) {
            _prefix = _prefix.substring(1);
        }
        System.out.println("deleting all keys with '" + _prefix + "' in bucket '" + _bucket + "'");
        List<S3ObjectSummary> objectListing = s3Service.listObjects(_bucket, _prefix).getObjectSummaries();
        long size = 0;
        for (S3ObjectSummary s3ObjectSummary : objectListing) {
            size += s3ObjectSummary.getSize();
            s3Service.deleteObject(_bucket, s3ObjectSummary.getKey());
        }
        System.out.println("deleted " + objectListing.size() + " objects with size of " + size + " bytes");
    }
}

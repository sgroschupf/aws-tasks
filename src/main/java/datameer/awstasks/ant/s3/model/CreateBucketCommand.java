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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class CreateBucketCommand extends S3Command {

    private String _name;
    private String _location;
    private boolean _emptyIfExistent;
    private boolean _normalizeName;

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

    public void setEmptyIfExistent(boolean emptyIfExistent) {
        _emptyIfExistent = emptyIfExistent;
    }

    public boolean isEmptyIfExistent() {
        return _emptyIfExistent;
    }

    public void setNormalizeName(boolean normalizeName) {
        _normalizeName = normalizeName;
    }

    public boolean isNormalizeName() {
        return _normalizeName;
    }

    @Override
    public void execute(AmazonS3 s3Service) {
        String name = getNormalizedName();
        boolean doesBucketExist = s3Service.doesBucketExist(name);

        if (isEmptyIfExistent() && doesBucketExist) {
            List<S3ObjectSummary> s3Objects = s3Service.listObjects(name).getObjectSummaries();
            for (S3ObjectSummary s3Object : s3Objects) {
                s3Service.deleteObject(name, s3Object.getKey());
            }
            doesBucketExist = false;
        }
        if (!doesBucketExist) {
            try {
                s3Service.createBucket(name, _location);
                System.out.println("created bucket '" + name + "'");
            } catch (Exception e) {
                throw new RuntimeException("failed to create bucket '" + name + "'", e);
            }
        }
    }

    private String getNormalizedName() {
        if (isNormalizeName()) {
            return normalizeName(_name);
        }
        return _name;
    }

    private String normalizeName(String name) {
        name = name.toLowerCase();
        name = name.replace(' ', '-');
        return name;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + getNormalizedName() + "]";
    }

}

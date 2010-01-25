/**
 * Copyright 2010 the originimport java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;
plicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dm.awstasks.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

public class Ec2Util {

    public static ReservationDescription findByGroup(Jec2 ec2, String securityGroup, String mode) throws EC2Exception {
        List<ReservationDescription> reservationDescriptions = ec2.describeInstances(Collections.EMPTY_LIST);
        List<ReservationDescription> matchingReservationDescriptions = new ArrayList<ReservationDescription>(3);
        for (ReservationDescription reservationDescription : reservationDescriptions) {
            if (reservationDescription.getGroups().contains(securityGroup) && reservationDescription.getInstances().get(0).getState().equals(mode)) {
                matchingReservationDescriptions.add(reservationDescription);
            }
        }
        if (matchingReservationDescriptions.size() > 1) {
            throw new EC2Exception("found more then one instance group for security group '" + securityGroup + "' and with instances in '" + mode + "' mode");
        } else if (matchingReservationDescriptions.isEmpty()) {
            return null;
        }
        return matchingReservationDescriptions.get(0);
    }

    public static ReservationDescription reloadReservationDescription(Jec2 ec2, ReservationDescription reservationDescription) throws EC2Exception {
        List<String> instanceIds = getInstanceIds(reservationDescription);
        List<ReservationDescription> reservationDescriptions = ec2.describeInstances(instanceIds);
        if (reservationDescriptions.size() > 1) {
            throw new IllegalStateException("found more then one reservation description for thes instances: " + instanceIds);
        }
        if (reservationDescriptions.isEmpty()) {
            throw new IllegalStateException("found no reservation description for these instances: " + instanceIds);
        }
        return reservationDescriptions.get(0);
    }

    public static List<String> getInstanceIds(ReservationDescription reservationDescription) {
        List<Instance> instances = reservationDescription.getInstances();
        List<String> instanceIds = new ArrayList<String>(instances.size());
        for (Instance instance : instances) {
            instanceIds.add(instance.getInstanceId());
        }
        return instanceIds;
    }

}

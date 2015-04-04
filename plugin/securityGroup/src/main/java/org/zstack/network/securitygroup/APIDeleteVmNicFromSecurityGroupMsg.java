package org.zstack.network.securitygroup;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.vm.VmNicVO;

import java.util.List;

/**
 * @api
 *
 * delete a vm nic from security group
 *
 * @category security group
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.securitygroup.APIDeleteVmNicFromSecurityGroupMsg": {
"securityGroupUuid": "cfc28caea12649b184f4990ca7265d98",
"vmNicUuid": "d87f0d53dbcb4befbcaade738058e3d9",
"session": {
"uuid": "ffa14cc5d36d40fea22d062f0ed724ba"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.securitygroup.APIDeleteVmNicFromSecurityGroupMsg": {
"securityGroupUuid": "cfc28caea12649b184f4990ca7265d98",
"vmNicUuid": "d87f0d53dbcb4befbcaade738058e3d9",
"session": {
"uuid": "ffa14cc5d36d40fea22d062f0ed724ba"
},
"timeout": 1800000,
"id": "0cb5681956f04154855addf23d11d4c8",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIDeleteVmNicFromSecurityGroupEvent`
 */
public class APIDeleteVmNicFromSecurityGroupMsg extends APIMessage {
    /**
     * @desc security group uuid
     */
    @APIParam(resourceType = SecurityGroupVO.class)
    private String securityGroupUuid;
    /**
     * @desc vm nic uuid. See :ref:`VmNicInventory`
     */
    @APIParam(resourceType = VmNicVO.class, nonempty = true)
    private List<String> vmNicUuids;

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }
    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }
}

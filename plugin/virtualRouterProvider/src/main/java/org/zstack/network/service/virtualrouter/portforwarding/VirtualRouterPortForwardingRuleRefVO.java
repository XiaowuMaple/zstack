package org.zstack.network.service.virtualrouter.portforwarding;

import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.network.service.vip.VipVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
public class VirtualRouterPortForwardingRuleRefVO {
    @Id
    @Column
    private String uuid;
    
    @Column
    @ForeignKey(parentEntityClass = VipVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String vipUuid;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String virtualRouterVmUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }
}

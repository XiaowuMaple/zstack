package org.zstack.network.service.virtualrouter.dns;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.DnsStruct;
import org.zstack.header.network.service.NetworkServiceDnsBackend;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.RemoveDnsRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.SetDnsRsp;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 */
public class VirtualRouterDnsBackend implements NetworkServiceDnsBackend {
    private final CLogger logger = Utils.getLogger(VirtualRouterDnsBackend.class);

    @Autowired
    private VirtualRouterManager vrMgr;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    @Override
    public NetworkServiceProviderType getProviderType() {
        return VirtualRouterConstant.PROVIDER_TYPE;
    }

    private void applyDns(final Iterator<DnsStruct> it, final VmInstanceSpec spec, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final DnsStruct struct = it.next();
        final L3NetworkInventory l3 = struct.getL3Network();
        vrMgr.acquireVirtualRouterVm(struct.getL3Network(), spec, new ReturnValueCompletion<VirtualRouterVmInventory>(completion) {

            @Override
            public void success(final VirtualRouterVmInventory vr) {
                final List<VirtualRouterCommands.DnsInfo> dns = new ArrayList<VirtualRouterCommands.DnsInfo>(l3.getDns().size());
                for (String d : l3.getDns()) {
                    VirtualRouterCommands.DnsInfo dinfo = new VirtualRouterCommands.DnsInfo();
                    dinfo.setDnsAddress(d);
                    dns.add(dinfo);
                }

                VirtualRouterCommands.SetDnsCmd cmd = new VirtualRouterCommands.SetDnsCmd();
                cmd.setDns(dns);

                VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
                msg.setVmInstanceUuid(vr.getUuid());
                msg.setPath(VirtualRouterConstant.VR_SET_DNS_PATH);
                msg.setCommand(cmd);
                msg.setCheckStatus(true);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        VirtualRouterAsyncHttpCallReply re = reply.castReply();
                        SetDnsRsp ret = re.toResponse(SetDnsRsp.class);
                        if (ret.isSuccess()) {
                            logger.debug(String.format("successfully add dns entry[%s] to virtual router vm[uuid:%s, ip:%s]", struct, vr.getUuid(), vr.getManagementNic()
                                    .getIp()));
                            applyDns(it, spec, completion);
                        } else {
                            String err = String.format("virtual router[uuid:%s, ip:%s] failed to configure dns%s for L3Network[uuid:%s, name:%s], %s",
                                    vr.getUuid(), vr.getManagementNic().getIp(), struct, l3.getUuid(), l3.getName(), ret.getError());
                            logger.warn(err);
                            completion.fail(errf.stringToOperationError(err));
                        }
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void applyDnsService(List<DnsStruct> dnsStructList, VmInstanceSpec spec, Completion completion) {
        if (dnsStructList.isEmpty()) {
            completion.success();
            return;
        }

        applyDns(dnsStructList.iterator(), spec, completion);
    }


    private void releaseDns(final Iterator<DnsStruct> it, final VmInstanceSpec spec, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        DnsStruct struct = it.next();
        if (!vrMgr.isVirtualRouterRunningForL3Network(struct.getL3Network().getUuid())) {
            logger.debug(String.format("virtual router for l3Network[uuid:%s] is not running, skip releasing DNS", struct.getL3Network().getUuid()));
            releaseDns(it, spec, completion);
            return;
        }

        final VirtualRouterVmInventory vr = vrMgr.getVirtualRouterVm(struct.getL3Network());

        final List<VirtualRouterCommands.DnsInfo> info = new ArrayList<VirtualRouterCommands.DnsInfo>();
        for (String dns : struct.getDns()) {
            VirtualRouterCommands.DnsInfo i = new VirtualRouterCommands.DnsInfo();
            i.setDnsAddress(dns);
            info.add(i);
        }

        VirtualRouterCommands.RemoveDnsCmd cmd = new VirtualRouterCommands.RemoveDnsCmd();
        cmd.setDns(info);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setCheckStatus(true);
        msg.setPath(VirtualRouterConstant.VR_REMOVE_DNS_PATH);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("virtual router[name: %s, uuid: %s] failed to remove dns%s, because %s",
                            vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(info), reply.getError()));
                    //TODO: schedule job to clean up
                } else {
                    VirtualRouterAsyncHttpCallReply re = reply.castReply();
                    RemoveDnsRsp ret = re.toResponse(RemoveDnsRsp.class);
                    if (ret.isSuccess()) {
                        logger.warn(String.format("virtual router[name: %s, uuid: %s] successfully removed dns%s",
                                vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(info)));
                    } else {
                        logger.warn(String.format("virtual router[name: %s, uuid: %s] failed to remove dns%s, because %s",
                                vr.getName(), vr.getUuid(), JSONObjectUtil.toJsonString(info), ret.getError()));
                        //TODO: schedule job to clean up
                    }
                }

                releaseDns(it, spec, completion);
            }
        });
    }

    @Override
    public void releaseDnsService(List<DnsStruct> dnsStructList, VmInstanceSpec spec, NoErrorCompletion completion) {
        if (dnsStructList.isEmpty()) {
            completion.done();
            return;
        }

        releaseDns(dnsStructList.iterator(), spec, completion);
    }
}

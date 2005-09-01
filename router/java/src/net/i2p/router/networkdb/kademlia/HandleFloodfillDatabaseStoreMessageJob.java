package net.i2p.router.networkdb.kademlia;
/*
 * free (adj.): unencumbered; not under the control of others
 * Written by jrandom in 2003 and released into the public domain 
 * with no warranty of any kind, either expressed or implied.  
 * It probably won't make your computer catch on fire, or eat 
 * your children, but it might.  Use at your own risk.
 *
 */

import java.util.*;
import net.i2p.data.*;
import net.i2p.data.i2np.*;
import net.i2p.router.*;
import net.i2p.util.Log;

/**
 * Receive DatabaseStoreMessage data and store it in the local net db
 *
 */
public class HandleFloodfillDatabaseStoreMessageJob extends JobImpl {
    private Log _log;
    private DatabaseStoreMessage _message;
    private RouterIdentity _from;
    private Hash _fromHash;
    private FloodfillNetworkDatabaseFacade _facade;

    private static final int ACK_TIMEOUT = 15*1000;
    private static final int ACK_PRIORITY = 100;
    
    public HandleFloodfillDatabaseStoreMessageJob(RouterContext ctx, DatabaseStoreMessage receivedMessage, RouterIdentity from, Hash fromHash, FloodfillNetworkDatabaseFacade facade) {
        super(ctx);
        _log = ctx.logManager().getLog(getClass());
        ctx.statManager().createRateStat("netDb.storeHandled", "How many netDb store messages have we handled?", "NetworkDatabase", new long[] { 5*60*1000l, 60*60*1000l, 24*60*60*1000l });
        ctx.statManager().createRateStat("netDb.storeLeaseSetHandled", "How many leaseSet store messages have we handled?", "NetworkDatabase", new long[] { 5*60*1000l, 60*60*1000l, 24*60*60*1000l });
        ctx.statManager().createRateStat("netDb.storeRouterInfoHandled", "How many routerInfo store messages have we handled?", "NetworkDatabase", new long[] { 5*60*1000l, 60*60*1000l, 24*60*60*1000l });
        _message = receivedMessage;
        _from = from;
        _fromHash = fromHash;
        _facade = facade;
    }
    
    public void runJob() {
        if (_log.shouldLog(Log.DEBUG))
            _log.debug("Handling database store message");

        String invalidMessage = null;
        boolean wasNew = false;
        if (_message.getValueType() == DatabaseStoreMessage.KEY_TYPE_LEASESET) {
            getContext().statManager().addRateData("netDb.storeLeaseSetHandled", 1, 0);
   
            try {
                LeaseSet ls = _message.getLeaseSet();
                // mark it as something we received, so we'll answer queries 
                // for it.  this flag does NOT get set on entries that we 
                // receive in response to our own lookups.
                ls.setReceivedAsPublished(true);
                LeaseSet match = getContext().netDb().store(_message.getKey(), _message.getLeaseSet());
                if (match == null) {
                    wasNew = true;
                } else {
                    wasNew = false;
                    match.setReceivedAsPublished(true);
                }
            } catch (IllegalArgumentException iae) {
                invalidMessage = iae.getMessage();
            }
        } else if (_message.getValueType() == DatabaseStoreMessage.KEY_TYPE_ROUTERINFO) {
            getContext().statManager().addRateData("netDb.storeRouterInfoHandled", 1, 0);
            if (_log.shouldLog(Log.INFO))
                _log.info("Handling dbStore of router " + _message.getKey() + " with publishDate of " 
                          + new Date(_message.getRouterInfo().getPublished()));
            try {
                Object match = getContext().netDb().store(_message.getKey(), _message.getRouterInfo());
                wasNew = (null == match);
                getContext().profileManager().heardAbout(_message.getKey());
            } catch (IllegalArgumentException iae) {
                invalidMessage = iae.getMessage();
            }
        } else {
            if (_log.shouldLog(Log.ERROR))
                _log.error("Invalid DatabaseStoreMessage data type - " + _message.getValueType() 
                           + ": " + _message);
        }
        
        if (_message.getReplyToken() > 0) 
            sendAck();
        
        if (_from != null)
            _fromHash = _from.getHash();
        if (_fromHash != null) {
            if (invalidMessage == null) {
                getContext().profileManager().dbStoreReceived(_fromHash, wasNew);
                getContext().statManager().addRateData("netDb.storeHandled", 1, 0);
                if (FloodfillNetworkDatabaseFacade.floodfillEnabled(getContext()) && (_message.getReplyToken() > 0) ) {
                    if (_message.getValueType() == DatabaseStoreMessage.KEY_TYPE_LEASESET)
                        _facade.flood(_message.getLeaseSet());
                    else
                        _facade.flood(_message.getRouterInfo());
                }
            } else {
                if (_log.shouldLog(Log.WARN))
                    _log.warn("Peer " + _fromHash.toBase64() + " sent bad data: " + invalidMessage);
            }
        }
    }
    
    private void sendAck() {
        DeliveryStatusMessage msg = new DeliveryStatusMessage(getContext());
        msg.setMessageId(_message.getReplyToken());
        msg.setArrival(getContext().clock().now());
        TunnelInfo outTunnel = selectOutboundTunnel();
        if (outTunnel == null) {
            if (_log.shouldLog(Log.WARN))
                _log.warn("No outbound tunnel could be found");
            return;
        } else {
            getContext().tunnelDispatcher().dispatchOutbound(msg, outTunnel.getSendTunnelId(0), _message.getReplyTunnel(), _message.getReplyGateway());
        }
    }

    private TunnelInfo selectOutboundTunnel() {
        return getContext().tunnelManager().selectOutboundTunnel();
    }
 
    public String getName() { return "Handle Database Store Message"; }
    
    public void dropped() {
        getContext().messageHistory().messageProcessingError(_message.getUniqueId(), _message.getClass().getName(), "Dropped due to overload");
    }
}
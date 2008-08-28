<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<html><head>
<title>I2P Router Console - config peers</title>
<link rel="stylesheet" href="default.css" type="text/css" />
</head><body>

<%@include file="nav.jsp" %>
<%@include file="summary.jsp" %>

<div class="main" id="main">
 <%@include file="confignav.jsp" %>
  
 <jsp:useBean class="net.i2p.router.web.ConfigPeerHandler" id="formhandler" scope="request" />
 <jsp:setProperty name="formhandler" property="*" />
 <jsp:setProperty name="formhandler" property="contextId" value="<%=(String)session.getAttribute("i2p.contextId")%>" />
 <font color="red"><jsp:getProperty name="formhandler" property="errors" /></font>
 <i><jsp:getProperty name="formhandler" property="notices" /></i>
 


 <jsp:useBean class="net.i2p.router.web.ConfigPeerHelper" id="peerhelper" scope="request" />
 <jsp:setProperty name="peerhelper" property="contextId" value="<%=(String)session.getAttribute("i2p.contextId")%>" />

 <% String peer = "";
    if (request.getParameter("peer") != null)     
        peer = request.getParameter("peer");
 %>
 
 <form action="configpeer.jsp" method="POST">
 <% String prev = System.getProperty("net.i2p.router.web.ConfigPeerHandler.nonce");
    if (prev != null) System.setProperty("net.i2p.router.web.ConfigPeerHandler.noncePrev", prev);
    System.setProperty("net.i2p.router.web.ConfigPeerHandler.nonce", new java.util.Random().nextLong()+""); %>
 <input type="hidden" name="nonce" value="<%=System.getProperty("net.i2p.router.web.ConfigPeerHandler.nonce")%>" />
 <hr />
 <p>
 <a name="sh"> </a>
 <a name="unsh"> </a>
 <a name="bonus"> </a>
 <h2>Manual Peer Controls</h2>
 Router Hash:
 <input type="text" size="55" name="peer" value="<%=peer%>" />
 <h3>Manually Shitlist / Unshitlist a Router</h3>
 Shitlisting will prevent the participation of this peer in tunnels you create.
 <p>
 <input type="submit" name="action" value="Shitlist peer until restart" />
 <input type="submit" name="action" value="Unshitlist peer" />
 <% if (! "".equals(peer)) { %>
    <font color="blue">&lt;---- click to verify action</font>
 <% } %>
 </p>

 <h3>Adjust Profile Bonuses</h3>
 Bonuses may be positive or negative, and affect the peer's inclusion in Fast and High Capacity tiers.
 Fast peers are used for client tunnels, and High Capacity peers are used for some exploratory tunnels.
 Current bonuses are displayed on the <a href="profiles.jsp">profiles page</a>.
 <p>
 <% long speed = 0; long capacity = 0;
    if (! "".equals(peer)) {
        // get existing bonus values?
    }
 %>
 Speed:
 <input type="text" size="8" name="speed" value="<%=speed%>" />
 Capacity:
 <input type="text" size="8" name="capacity" value="<%=capacity%>" />
 <input type="submit" name="action" value="Adjust peer bonuses" />
 </p>
 </form>

 <hr />
 <a name="shitlist"> </a>
 <jsp:useBean class="net.i2p.router.web.ProfilesHelper" id="profilesHelper" scope="request" />
 <jsp:setProperty name="profilesHelper" property="contextId" value="<%=(String)session.getAttribute("i2p.contextId")%>" />
 <jsp:getProperty name="profilesHelper" property="shitlistSummary" />


</div>

</body>
</html>
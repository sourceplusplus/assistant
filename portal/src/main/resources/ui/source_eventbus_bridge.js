var eb = new EventBus('http://localhost:7529/eventbus');
eb.enableReconnect(true);

var getPortalUuid = findGetParameter("portal_uuid");
var portalUuid = (getPortalUuid) ? getPortalUuid : null;
var getRequiresRegistration = findGetParameter("requires_registration");
var requiresRegistration = (getRequiresRegistration) ? getRequiresRegistration : false;
var traceOrderType = findGetParameter("order_type");
if (traceOrderType) {
    traceOrderType = traceOrderType.toUpperCase();
}
var getExternal = findGetParameter("external");
var externalPortal = (getExternal) ? (getExternal === 'true') : false;
var getDarkMode = findGetParameter("dark_mode");
var darkMode = (getDarkMode) ? (getDarkMode === 'true') : false;
var getHideOverviewTab = findGetParameter("hide_overview_tab");
var hideOverviewTab = (getHideOverviewTab) ? (getHideOverviewTab === 'true') : false;

var mainGetQuery = '?portal_uuid=' + portalUuid;
var mainGetQueryWithoutPortalUuid = "";
if (traceOrderType) {
    mainGetQueryWithoutPortalUuid += '&order_type=' + traceOrderType;
}
if (externalPortal) {
    mainGetQueryWithoutPortalUuid += '&external=true';
}
if (darkMode) {
    mainGetQueryWithoutPortalUuid += '&dark_mode=true';
}
if (hideOverviewTab) {
    mainGetQueryWithoutPortalUuid += '&hide_overview_tab=true';
}
mainGetQuery += mainGetQueryWithoutPortalUuid;

function findGetParameter(parameterName) {
    let result = null, tmp = [];
    location.search
        .substr(1)
        .split("&")
        .forEach(function (item) {
            tmp = item.split("=");
            if (tmp[0] === parameterName) result = decodeURIComponent(tmp[1]);
        });
    return result;
}

function clickedViewAsExternalPortal() {
    eb.send('ClickedViewAsExternalPortal', {
        'portal_uuid': portalUuid
    }, function (error, message) {
        window.open(window.location.href.split('?')[0] + '?portal_uuid=' + message.body.portal_uuid
            + '&external=true' + mainGetQueryWithoutPortalUuid, '_blank');
    });
}

function portalConnected() {
    console.log("Source++ Portal successfully connected to eventbus bridge");
    if (requiresRegistration) {
        eb.send("REGISTER_PORTAL", {
            'app_uuid': findGetParameter("app_uuid"),
            'artifact_qualified_name': findGetParameter("artifact_qualified_name")
        }, function (error, message) {
            window.open(window.location.href.split('?')[0] + '?portal_uuid=' + message.body.portal_uuid
                + mainGetQueryWithoutPortalUuid, '_self');
        });
    } else {
        window.setInterval(keepPortalAlive, 60000 * 4);
    }
}

function keepPortalAlive() {
    eb.send('KeepAlivePortal', {'portal_uuid': portalUuid});
    portalLog("Sent portal keep alive request. Portal UUID: " + portalUuid);
}

function portalLog(message) {
    console.log(message);
    eb.send('PortalLogger', message);
}
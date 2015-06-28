/**
 * @param {String} name
 * @param {String} color
 * @param {Level} role
 * @param {Level} globalRole
 * @param {String=} service
 * @param {String=} serviceRes
 * @constructor
 */
var User = function(name, color, role, globalRole, service, serviceRes) {
    this.name = name;
    if (color) {
        this.color = color;
    } else {
        this.color = "#000000";
    }
    if (role) {
        this.role = role;
    } else {
        this.role = levels.GUEST;
    }
    this.globalRole = globalRole;
    this.online = 0;
    this.banned = false;
    this.timedOut = false;
    this.service = service;
    this.serviceRes = serviceRes;
};

/**
 * @returns {*}
 */
User.prototype.rgbColor = function() {
    return hexToRgb(this.color);
};

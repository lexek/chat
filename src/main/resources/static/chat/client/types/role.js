/**
 * @param {string} title
 * @param {number} value
 * @constructor
 */
Level = function(title, value) {
    this.title = title;
    this.value = value;
};

Level.prototype.valueOf = function() {
    return this.value;
};

levels = {
    'GUEST': new Level("GUEST", 1),
    'USER': new Level("USER", 2),
    'MOD': new Level("MOD", 3),
    'ADMIN': new Level("ADMIN", 4)
};

globalLevels = {
    'UNAUTHENTICATED': new Level("UNAUTHENTICATED", 0),
    'GUEST': new Level("GUEST", 1),
    'USER_UNCONFIRMED': new Level("USER_UNCONFIRMED", 2),
    'USER': new Level("USER", 2),
    "SUPPORTER": new Level("SUPPORTER", 3),
    'MOD': new Level("MOD", 4),
    'ADMIN': new Level("ADMIN", 5),
    'SUPERADMIN': new Level("SUPERADMIN", 6)
};

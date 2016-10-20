var exec = require("cordova/exec");

/**
 * This is a global variable called wakeup exposed by cordova
 */    
var Wakeup = function(){};

Wakeup.prototype._listener = {};

Wakeup.prototype.wakeup = function(options, callback, scope) {
    this.exec('wakeup', options, callback, scope);
};
Wakeup.prototype.snooze = function(options, callback, scope) {
    this.exec('snooze', options, callback, scope);
};

/**
 * Start alarm
 *
 * @param {Function} callback
 *      A function to be called after the notification has been cleared
 * @param {Object?} scope
 *      The scope for the callback function
 */
Wakeup.prototype.alarm = function (callback, scope) {
    this.exec('alarm', null, callback, scope);
};

/**
 * Fire event with given arguments.
 *
 * @param {String} event
 *      The event's name
 * @param {args*}
 *      The callback's arguments
 */
Wakeup.prototype.fireEvent = function(event) {
    var args     = Array.apply(null, arguments).slice(1),
        listener = this._listener[event];

    if (!listener)
        return;

    for (var i = 0; i < listener.length; i++) {
        var fn    = listener[i][0],
            scope = listener[i][1];

        fn.apply(scope, args);
    }
};

/**
 * Create callback, which will be executed within a specific scope.
 *
 * @param {Function} callbackFn
 *      The callback function
 * @param {Object} scope
 *      The scope for the function
 *
 * @return {Function}
 *      The new callback function
 */
Wakeup.prototype.createCallbackFn = function (callbackFn, scope) {

    if (typeof callbackFn != 'function')
        return;

    return function () {
        callbackFn.apply(scope || this, arguments);
    };
};

/**
 * Execute the native counterpart.
 *
 * @param {String} action
 *      The name of the action
 * @param args[]
 *      Array of arguments
 * @param {Function} callback
 *      The callback function
 * @param {Object} scope
 *      The scope for the function
 */
Wakeup.prototype.exec = function (action, args, callback, scope) {
    var fn = this.createCallbackFn(callback, scope),
        params = [];

    if (Array.isArray(args))
    {
        params = args;
    }
    else if (args)
    {
        params.push(args);
    }

    exec(fn, null, 'WakeupPlugin', action, params);
};

/**
 * Register callback for given event.
 *
 * @param {String} event
 *      The event's name
 * @param {Function} callback
 *      The function to be exec as callback
 * @param {Object?} scope
 *      The callback function's scope
 */
Wakeup.prototype.on = function (event, callback, scope) {

    if (typeof callback !== "function")
        return;

    if (!this._listener[event]) {
        this._listener[event] = [];
    }

    var item = [callback, scope || window];

    this._listener[event].push(item);
};

module.exports = new Wakeup();

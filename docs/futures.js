futures = function() {
  class Exception {
    constructor(message) {
      this._message = message;
    }

    static wrap(exc) {
      if (exc == null) {
        return new Exception("Nullpointer");
      } else if (exc instanceof Exception) {
        return exc;
      } else {
        return new Exception(exc);
      }
    }

    getMessage() {
      return this.toString();
    }

    toString() {
      return this._message;
    }
  }

  function _assert(expr, msg) {
    if (!expr) {
      throw new Exception("Assertion failed: " + msg);
    }
  }

  function _assert_exc(exc) {
    _assert(exc instanceof Exception, "Expected an exception but found " + exc);
  }

  function _assert_future(future) {
    _assert(future instanceof CompletableFuture, "Expected an future but found " + future);
  }

  function _assert_result(result) {
    _assert(result instanceof Result, "Expected a result but found " + result);
  }

  function _assert_allowed(name) {
    if (_globalAllowed) {
      _assert(_globalAllowed.has(name), "Not allowed to call function: " + name);
    }
  }

  class Result {
    constructor(value, exception) {
      this._value = value;
      this._exception = exception;
    }

    static exception(exc) {
      return new Result(null, Exception.wrap(exc));
    }

    static value(value) {
      return new Result(value, null);
    }

    isException() {
      return this._exception != null;
    }

    isValue() {
      return this._exception == null;
    }

    equals(other) {
      return this._exception == other._exception && this._value == other._value;
    }

    toString() {
      if (this._exception != null) {
        return "Exception('" + this._exception.getMessage() + "')";
      }
      return "Value('" + this._value + "')";
    }
  }

  class CompletableFuture {
    constructor() {
      this._result = null;
      this._callbacks = [];
    }

    static completedFuture(value) {
      var newFuture = new CompletableFuture();
      newFuture.complete(value);
      return newFuture;
    }

    static failedFuture(exc) {
      _assert(_globalLanguageLevel >= 9, "Requires Java 9+");
      _assert_exc(exc);
      var newFuture = new CompletableFuture();
      newFuture.completeExceptionally(exc);
      return newFuture;
    }

    static _completedResult(result) {
      var f = new CompletableFuture();
      f._completeResult(result);
      return f;
    }

    // fun: Exception => Future
    exceptionallyCompose(fun) {
      _assert(_globalLanguageLevel >= 12, "Requires Java 12+");
      _assert_allowed("exceptionallyCompose");
      return this
        ._thenApply(x => CompletableFuture.completedFuture(x))
        ._exceptionally(fun)
        ._thenCompose(x => x);
    }

    // fun: (Value, Value) => Value
    thenCombine(other, fun) {
      _assert_allowed("thenCombine");
      return this._thenCompose(x => other._thenApply(y => fun(x, y)));
    }

    // fun: Exception => Value
    exceptionally(fun) {
      _assert_allowed("exceptionally");
      return this._exceptionally(fun);
    }

    _exceptionally(fun) {
      return this._handle((value, exc) => {
        if (exc != null) {
          return fun(exc);
        } else {
          return value;
        }
      });
    }

    // fun: Value => Value
    thenApply(fun) {
      _assert_allowed("thenApply");
      return this._thenApply(fun);
    }

    _thenApply(fun) {
      return this._handle((value, exc) => {
        if (exc != null) {
          throw exc;
        } else {
          return fun(value);
        }
      });
    }

    // fun: Value => Future
    thenCompose(fun) {
      _assert_allowed("thenCompose");
      return this._thenCompose(fun);
    }

    _thenCompose(fun) {
      return this._thenApply(fun)._dereference();
    }

    // fun: (Value, Exception) => Value
    handle(fun) {
      _assert_allowed("handle");
      return this._handle(fun);
    }

    _handle(fun) {
      return this._handleResult(result => {
        if (result.isValue()) {
          return Result.value(fun(result._value, null));
        } else {
          return Result.value(fun(null, result._exception));
        }
      });
    }

    // fun: Result => Result
    _handleResult(fun) {
      var newFuture = new CompletableFuture();
      this._addCallback(result => {
        try {
          var result2 = fun(result);
          _assert_result(result2);
          newFuture._completeResult(result2);
        } catch (err) {
          newFuture._completeResult(Result.exception(err));
        }
      });
      return newFuture;
    }

    _dereference() {
      var newFuture = new CompletableFuture();
      this._addCallback(result => {
        try {
          if (result.isValue()) {
            var future = result._value;
            _assert_future(future);
            future._addCallback(result2 => newFuture._completeResult(result2))
          } else {
            newFuture._completeResult(result);
          }
        } catch (err) {
          newFuture._completeResult(Result.exception(err));
        }
      });
      return newFuture;
    }

    // fun: Result => Void
    // fun may not throw errors
    _addCallback(fun) {
      if (this.isComplete()) {
        fun(this._result);
      } else {
        this._callbacks.push(fun);
      }
    }

    isComplete() {
      return this._result != null;
    }

    getResult() {
      if (!this.isComplete()) {
        throw "Future is not completed";
      }
      return this._result;
    }

    getValue() {
      var res = getResult();
      if (res.isException()) {
        throw "Future completed exceptionally";
      }
      return res._value;
    }

    complete(value) {
      this._completeResult(Result.value(value));
    }

    completeExceptionally(exc) {
      _assert_exc(exc);
      this._completeResult(Result.exception(exc));
    }

    _completeResult(result) {
      if (this._result == null) {
        this._result = result;
      }
      var callbacks = this._callbacks;
      this._callbacks = [];
      for (var callback of callbacks) {
        try {
          callback(result);
        } catch (err) {
          console.log("Unexpected error from callback: " + err.toString());
        }
      }
    }

    toString() {
      if (this._result != null) {
        return "CompleteFuture(" + this._result.toString() + ")";
      }
      return "IncompleteFuture";
    }

  }

  return {
    "CompletableFuture": CompletableFuture,
    "Result": Result
  }
}()
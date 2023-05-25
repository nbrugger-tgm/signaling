## Signaling

This in an **_experiment_** that re-creates the basis of **SolidJS** in Java:
 Signals and effects.

The target was to have a lib that allows this:
```java
var loggedIn = cx.createSignal(false);
var secretValue = cx.createSignal("s3cr3t");

cx.createEffect(() -> {
    print("Hi! ")
    if (loggedIn.get()) {
        println("The secret is : "+secretValue.get());
    }else{
        println("You shall not see the secret!");
    }
});

secretValue.set("other");//nothing happens
loggedIn.set(true);//"Hi! The secret is : other" is logged
secretValue.set("s3cr3t");//"Hi! The secret is : s3cr3t" is logged
```

Also support for nested effects (caused a lot of headache!) was important.
Example:
```java
cx.createEffect(() -> {
    if (loggedIn.get() && expensiveApiCall()) {
        cx.createEffect(()->println("The secret is : "+secretValue.get()));
    }else{
        println("You shall not see the secret!");
    }
});
```
This causes that the `expensiveApiCall`  login check is only executed when `loggedIn` changes and not when `secretValue`
changes since it only triggers the inner event!

All the examples above are runnable! See `UseEffectTest.java`! The behaviour is nearly exactly the same as SolidJS (a few performance improvements are missing)

# Features
I have close to 100% codecoverage but not casecoverage so don't take everything for granted
- Defered execution of nested effects for performance
- Nested effects actualy work - amazing i know
- No unsubscribing needed! As soon as a signal goes out of scope all related effects are removed!
- Noop effects are cleaned automatically - no waste.
- Thread-safe (but blocking!)

# Licence
Do whatever the f you want with it
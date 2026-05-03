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

The idea was to mimic SolidJS behavior as well as possible so most things behave like in Solid.

# Features

While this is inspired by SolidJS a lot of what solidJS offers makes no sense in a JVM world. 
But at the other hand there are also things that the Java world needs that JS does not so here are the features that are added on-top of the replication.

### Proxy generation for complex signals

In case you have a bean such as this ([read more](processor/reactive-proxy.md))
```java
class TodoItem {
    boolean done;
    String text;
}
```
you cannot granularly update each field for fine-grained reactivity. By using `@Reactive` you can create a reactive complex signals
```java
@Reactive
interface TodoItem {
    boolean done();
    String text();
    void setText(String text);
    void setDone(boolean done);
}
```
which will create an implementation where each field is backed by a signal
```java
TodoItem item = ProxyFactory.create(cx, new TodoItem$Init("My task", false));
cx.createEffect(()->System.out.println("Task completion changed: "+item.done()));
cx.createEffect(()->System.out.println("Task text changed: "+item.text()));
item.setText("xyz"); //sout: Task text changed: xyz
item.setDone(true);//sout: Task completion changed: true
```
Should you happen to dislike codegeneration (compile-time not runtime!) your best alternative is this

```java
record TodoItem(
        Signal<Boolean> done, 
        Signal<String> text
) {
}

TodoItem item = new TodoItem(
        cx.createSignal(false),
        cx.createSignal("MyTask")
);
cx.createEffect(()->System.out.println("Task completion changed: "+item.done.get()));
cx.createEffect(()->System.out.println("Task text changed: "+item.text.get()));
item.done.set(false);
item.text.set(true);
```

# Licence
Free for any kind of use
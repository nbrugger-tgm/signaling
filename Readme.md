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

## Webapp

As a proove of concept i built a transpiled version (eg. java compiled into javascript) that uses my "solid in java" to create a simple client side app!
```java
package example;
public class MainClass {
    static Context cx = Context.create();
    static HTMLDocument document = HTMLDocument.current();
    static JSX jsx = new JSX(document, cx);
    public static void main(String[] args) {

        var count = cx.createSignal(0);
        var div = jsx.div(
                jsx.h1("Java TeaVM Counter"),
                Counter(count),
                jsx.span(()->" -> "+count.get())
        );

        document.getBody().appendChild(div);
    }

    private static HTMLElement Counter(Signal<Integer> count) {
        System.out.println("Create counter");
        return jsx.button(
                count, //Text of the button
                e -> count.update(c -> c+1) //click listener
        );
    }
}
```
**Thats all!** In java one cannot create JSX syntax so i had to stick with methods.

For comparison here the real solid version:
```typescript jsx
const App = () => {
  const [count, setCount] = createSignal(0);
  return <div>
    <h1>Java TeaVm Counter</h1>
    <Counter count={count()} setCount={setCount}/>
    <span> -> {count()}</span>
  </div>
};
const Counter = (props: {count: number, setCount: (func: (old:number)=>number)=>void}) =>{
  var increment = ()=> props.setCount((c:number) => c+1)
  return <button onClick={increment}>{props.count}</button>
}


render(() => <App />, document.getElementById("app"));
```

## Running this weird thing
```shell
./gradlew package
```
open `webapp/build/app/index.html` in your browser - BOOM

A fully and truly reactive Client side app in your browser - PURELY written in Java!


# Disclaimer
You should for many **many** obvious reasons NOT do this in production - not even in dev!
This is an **experiment** - but it is very interesting and i learned a f * * *  ton. I hope you enjoy this. 
I am amazed about how well and easy TeaVM (transpiles java to js) works. In the future [googles j2cl](https://github.com/google/j2cl) could be used to do what i do at production scale. There is just not enought infra jet - but in theory j2cl is production ready!

# Features
I have close to 100% codecoverage but not casecoverage so don't take everything for granted
- Defered execution of nested effects for performance
- Nested effects actualy work - amazing i know
- No unsubscribing needed! As soon as a signal goes out of scope all related effects are removed!
- Noop effects are cleaned automatically - no waste.
- Thread-safe (but blocking!)

# Licence
Do whatever the f you want with it
class Generics {

    class TypeWithParameter<X>(X init) {
        mutable X x := init;
        X process(X input) { return input }
    }
    
    class TypeWithMultipleParameters<X,Y>(Map<X,Y> map) {
        Y y(X x) { return map[y] }
    }
    
    class TypeWithInParameter<in X>(String toString(X x)) {
        String consume(X x) { return toString(x) }
    }
    
    class TypeWithOutParameter<out X>(X x) {
        X produce() { return x }
        X x = x;
    }
    
    interface TypeWithVariantParameters<out X, in Y, Z> {
        void consume(X x);
        X produce();
        mutable Z z;
    }
    
    class TypeWithLowerBoundParameter<X>(X produce())
            where X >= String {
        String s = produce();
    }

    class TypeWithUpperBoundParameter<X>(void accept(X x), X xx)
            where X <= String {
        accept(xx);
    }
        
    interface TypeWithSubtypeParameter<X>
            where X = subtype {
        X add(X x);
        X multiply(X x);
    }
        
    class TypeWithConstructableParameter<X>(String s, Natural n)
            where X(String s, Natural n) {
        X x = X(s,n);
    }

    class TypeWithMultipleParameterConstraints<X>(String s, Natural n)
            where X >= String & X(String s, Natural n) {
        String sn = X(s,n);
    }


    X methodWithParameter<X>(X x) { return x }
    
    Entry<X,Y> methodWithMultipleParameters<X,Y>(X x, Y y) { return Entry(x, y) }
    
    String methodWithLowerBoundParameter<X>(X produce())
        where X >= String { return produce() }

    void methodWithUpperBoundParameter<X>(void accept(X x), X xx)
        where X <= String { accept(xx); }
        
    X methodWithConstructableParameter<X>(String s, Natural n)
        where X(String s, Natural n) { return X(s,n) }

    String methodWithMultipleParameterConstraints<X>(String s, Natural n)
        where X >= String & X(String s, Natural n) { return X(s,n) }


    interface Processor<out X, in Y> {
        X process(Y y);
    }
    
    class ProcessorImpl<out X, in Y>(X p(Y y)) {
        X process(Y y) {
            return p(y)
        }
    }
    
    String stringify<Y>(Y y) where Y>=Number { return $y }
    String zero = stringify<Natural>(0);

    Processor<String, Natural> p = ProcessorImpl<String, Natural>(stringify);
    String one = p.process(1);
    
    void output<Y>(Y value, Processor<String,Y> p) where Y>=Number {
        log.info(p.process(value));
    }
    
    output(2,p);

}
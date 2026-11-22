package demo;

class TargetServiceA { public void doA() {} }
class TargetServiceB { public void doB() {} }
class TargetServiceC { public void doC() {} }
class TargetServiceD { public void doD() {} }

public class FeatureEnvyExample {

    private TargetServiceA a = new TargetServiceA();
    private TargetServiceB b = new TargetServiceB();
    private TargetServiceC c = new TargetServiceC();
    private TargetServiceD d = new TargetServiceD();

    // Excessive coupling to foreign classes (Feature Envy proxy)
    public void processExternalData() {
        a.doA();
        b.doB();
        c.doC();
        d.doD();
    }
}

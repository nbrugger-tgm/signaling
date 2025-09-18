package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.ContextTest;

class SetStackContextTest extends ContextTest {
    protected Context init(){
        return new SetStackContext();
    }
}
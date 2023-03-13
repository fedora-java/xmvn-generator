package org.fedoraproject.xmvn.generator.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;

public class RpmBuildContextTest {
    @Test
    public void testEval() {
        BuildContext bc = new RpmBuildContext();
        assertEquals("12", bc.eval("%[7+5]"));
        assertEquals("12", bc.eval("%(expr 7 + 5)"));
    }
}

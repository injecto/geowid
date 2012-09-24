package com.ecwid.geowid.daemon.tests;

import com.ecwid.geowid.daemon.resolvers.ResolveRecord;
import com.ecwid.geowid.daemon.resolvers.RuIpResolver;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Тест класса RuIpResolver
 */
public class RuIpResolverTest {
    @Test
    public void testResolve() throws Exception {
        RuIpResolver resolver = new RuIpResolver("", 60);

        ResolveRecord result = resolver.resolve("144.206.192.6");
        assertEquals(56.0, Math.ceil(result.getLat()));
        assertEquals(38.0, Math.ceil(result.getLng()));
    }
}

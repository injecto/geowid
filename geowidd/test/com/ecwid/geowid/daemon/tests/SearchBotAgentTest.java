package com.ecwid.geowid.daemon.tests;

import com.ecwid.geowid.daemon.utils.SearchBotAgent;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * тест класса SearchBotAgent
 */
public class SearchBotAgentTest {
     @Test
    public void testSearchBotAgent() throws Exception {
         SearchBotAgent agent = new SearchBotAgent()
                 .addAgentRegExp(SearchBotAgent.Google);

         assertTrue(agent.isSearchBot("info Sep 10 06:43:38  - ErrorPageContro handleRequ Error " +
                 "in printeriekspert.ecwid.com Referer: null /jsp/1403030/product?14852331 status: " +
                 "500 Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html) Server Error#012"));
         assertFalse(agent.isSearchBot("info Sep 10 06:45:35  - ErrorPageContro handleRequ Error in" +
                 " bowrotary.ecwid.com Referer: null /jsp/712083/catalog status: 500 Mozilla/5.0 " +
                 "(compatible; YandexBot/3.0; +http://yandex.com/bots) Server Error#012"));
    }
}

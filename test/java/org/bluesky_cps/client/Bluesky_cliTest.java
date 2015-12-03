package org.bluesky_cps.client;

import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.bluesky-cps.client.test.ExtraTest;

@Category(ExtraTest.class);
public class Bluesky_cliTest {

    private Bluesky_cli target = new Bluesky_cli("http://127.0.0.1:8189", "guest", "guest");

    @Test
    public void test() {
	//Test login.
	boolean isLoginSuccess = target.login();
	assertTrue(isLoginSuccess);

	//Test logout.
	boolean isLogoutSuccess = target.logout();
	assertTrue(isLogoutSuccess);

	//Test createBlueskyParam.
	String[] opts = {"noneFix", "edconnected"};
	String blueskyParamStr = target.createBlueskyParam("ls", opts);
	assertThat(blueskyParamStr, "/etLog?instruction=ls&opt1=noneFix&opt2=edconnected");

	//Test blueskyGet.
	//System.out.println(this.blueskyGet(blueskyParamStr));

	//Test list_ed.
	//System.out.println(this.list_ed());
    }
}

package ca.mcgill.sus.screensaver.datafetch;

import ca.mcgill.science.tepid.api.ITepidScreensaver;
import ca.mcgill.science.tepid.models.data.NameUser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class WindowsUserFetch extends UserFetch {

    public WindowsUserFetch(long _timeOutInterval, ITepidScreensaver _api) {
        super(_timeOutInterval, _api);
    }

    @Override
    protected FetchResult<NameUser> getCurrentUser() {
        String command = "powershell.exe \"Import-Module ActiveDirectory; $attributes = 'displayName', 'samAccountName', 'mail', 'name', 'givenName', 'surname';" +
                "Get-AdUser " + System.getenv("username") + " -Properties $attributes | select $attributes\"";

        Map<String, String> nameInformation = new HashMap<>();
        try {
            Process PsGetAdUser = Runtime.getRuntime().exec(command);
            BufferedReader stdout = new BufferedReader(new InputStreamReader(PsGetAdUser.getInputStream()));

            String rawLine;
            while ((rawLine = stdout.readLine()) != null) {
                String[] line = rawLine.split(":");
                if (line.length == 2) {
                    nameInformation.put(line[0].trim(), line[1].trim());
                }
            }
            stdout.close();
        } catch (Exception e) {
            System.err.println("Error fetching user info using powershell");
            System.err.println(e.getLocalizedMessage());
            return new FetchResult<>(null, false);
        }

        System.out.println(nameInformation);

        NameUser user = new NameUser();
        user.setDisplayName(nameInformation.get("displayName"));
        user.setGivenName(nameInformation.get("givenName"));
        user.setLastName(nameInformation.get("surname"));
        user.setEmail(nameInformation.get("mail"));
        user.setShortUser(nameInformation.get("samAccountName"));

        if (user.getShortUser() == null) {
            user.setShortUser(System.getenv("username"));
        }
        return new FetchResult<>(user);
    }
}

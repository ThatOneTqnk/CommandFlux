# CommandFlux

A simple framework to make Brigaider commands for Bukkit.

Main class
```java
public class EpicPlugin extends JavaPlugin {
    private CommandFlux flux;

    @Override
    public void onEnable() {
        flux = new CommandFlux(this, "epic"); // epic would be the fallback prefix of the command

        // This tells flux how to deal with ExampleTeam
        flux.addLiteral(ExampleTeam.class, new FluxLiteral<ExampleTeam>() { 
            public List<String> getChoices() {
                return ExampleTeam.TEAMS.stream().map((team) -> team.name).collect(Collectors.toList());
            }

            public ExampleTeam toValue(String choice) {
                return ExampleTeam.findTeam(choice);
            }
        });

        flux.registerCommands(ExampleCommands.class);
    }
}
```

Commands class
```java
public class ExampleCommands {

    @FluxHandle(aliases = {"join"}, paramNames = {"team"}, min = 1)
    public static boolean join(CommandSender sender, ExampleTeam team) {
        sender.sendMessage(team.name);
        return true;
    }

    @FluxHandle(aliases = {"fluxtest"}, paramNames = {"range", "message"}, min = 1)
    public static boolean shout(CommandSender sender, @LongRange(minBound = 1L, maxBound = 2L) Long selection, @Greedy String message) {
        Bukkit.broadcastMessage("Long is " + selection + " and message is " + message);
        return true;
    }

}
```

Custom Type definition:

ExampleTeam
```java
public class ExampleTeam {
    public String name;

    public static final Set<ExampleTeam> TEAMS;

    public static ExampleTeam findTeam(String teamName) {
        for (ExampleTeam team : TEAMS) {
            if (team.name.equals(teamName)) return team;
        }
        return null;
    }

    static {
        TEAMS = new HashSet<>();
        TEAMS.add(new ExampleTeam("Red"));
        TEAMS.add(new ExampleTeam("Blue"));
    }

    public ExampleTeam(String name) {
        this.name = name;
    }
}
```

Completion result:

![result](https://i.imgur.com/g4qfWOL.png)
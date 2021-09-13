package com.game.controller;

import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.service.PlayerService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@RestController
@RequestMapping("/rest/players")
public class PlayerRestController {

    private final PlayerService playerService;

    public PlayerRestController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public ResponseEntity<List<Player>> getAllPlayers(@RequestParam(required = false) String name,
                                                      @RequestParam(required = false) String title,
                                                      @RequestParam(required = false) Race race,
                                                      @RequestParam(required = false) Profession profession,
                                                      @RequestParam(required = false) Long after,
                                                      @RequestParam(required = false) Long before,
                                                      @RequestParam(required = false) Boolean banned,
                                                      @RequestParam(required = false) Integer minExperience,
                                                      @RequestParam(required = false) Integer maxExperience,
                                                      @RequestParam(required = false) Integer minLevel,
                                                      @RequestParam(required = false) Integer maxLevel,
                                                      @RequestParam(required = false) PlayerOrder order,
                                                      @RequestParam(required = false) Integer pageNumber,
                                                      @RequestParam(required = false) Integer pageSize) {

        Specification<Player> spec = makeSpecification(name, title, race, profession, after, before, banned,
                minExperience, maxExperience, minLevel, maxLevel);

        if (pageNumber == null) pageNumber = 0;
        if (pageSize == null) pageSize = 3;
        if (order == null) order = PlayerOrder.ID;

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(order.name().toLowerCase()));

        List<Player> playerList = this.playerService.getAllPlayers(spec, pageable);

        return new ResponseEntity<>(playerList, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayer(@PathVariable("id") Long playerID) {

        if (!isIdValid(playerID)) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Player player = this.playerService.getPlayerById(playerID);

        if (player == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(player, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Player> createPlayer(@RequestBody(required = false) Player player) {

        if (player == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        String name = player.getName();
        String title = player.getTitle();
        Race race = player.getRace();
        Profession profession = player.getProfession();
        Date birthday = player.getBirthday();
        Integer experience = player.getExperience();
        if (player.getBanned() == null) player.setBanned(false);

        if (!isNameValid(name) || !isTitleValid(title) || race == null || profession == null
                || !isDateValid(birthday) || !isExperienceValid(experience))
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Integer level = calculateLevel(experience);
        player.setLevel(level);
        player.setUntilNextLevel(calculateUntilNextLevel(experience, level));

        this.playerService.save(player);

        return new ResponseEntity<>(player, HttpStatus.OK);
    }

    @PostMapping("/{id}")
    public ResponseEntity<Player> updatePlayer(@PathVariable("id") Long playerID,
                                               @RequestBody(required = false) Player updatedPlayer) {

        if(!isIdValid(playerID) || updatedPlayer == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Player player = this.playerService.getPlayerById(playerID);

        if (player == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        String name = updatedPlayer.getName();
        String title = updatedPlayer.getTitle();
        Race race = updatedPlayer.getRace();
        Profession profession = updatedPlayer.getProfession();
        Date birthday = updatedPlayer.getBirthday();
        Boolean banned = updatedPlayer.getBanned();
        Integer experience = updatedPlayer.getExperience();

        if ((name != null && !isNameValid(name))
                || (title != null && !isTitleValid(title))
                || (birthday != null && !isDateValid(birthday))
                || (experience != null && !isExperienceValid(experience)))
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        if (name != null) player.setName(name);
        if (title != null) player.setTitle(title);
        if (race != null) player.setRace(race);
        if (profession != null) player.setProfession(profession);
        if (birthday != null) player.setBirthday(birthday);
        if (banned != null) player.setBanned(banned);
        if (experience != null) {
            player.setExperience(experience);
            Integer level = calculateLevel(experience);
            player.setLevel(level);
            player.setUntilNextLevel(calculateUntilNextLevel(experience, level));
        }

        this.playerService.save(player);

        return new ResponseEntity<>(player, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Player> deletePlayer(@PathVariable("id") Long playerID) {

        if (!isIdValid(playerID)) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Player player = this.playerService.getPlayerById(playerID);

        if (player == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        this.playerService.delete(playerID);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getPlayerCount(@RequestParam(required = false) String name,
                                                  @RequestParam(required = false) String title,
                                                  @RequestParam(required = false) Race race,
                                                  @RequestParam(required = false) Profession profession,
                                                  @RequestParam(required = false) Long after,
                                                  @RequestParam(required = false) Long before,
                                                  @RequestParam(required = false) Boolean banned,
                                                  @RequestParam(required = false) Integer minExperience,
                                                  @RequestParam(required = false) Integer maxExperience,
                                                  @RequestParam(required = false) Integer minLevel,
                                                  @RequestParam(required = false) Integer maxLevel) {

        Specification<Player> spec = makeSpecification(name, title, race, profession, after, before, banned,
                minExperience, maxExperience, minLevel, maxLevel);

        Integer playerCount = this.playerService.getCount(spec);

        return new ResponseEntity<>(playerCount, HttpStatus.OK);
    }

    private Specification<Player> makeSpecification(String name,
                                                    String title,
                                                    Race race,
                                                    Profession profession,
                                                    Long after,
                                                    Long before,
                                                    Boolean banned,
                                                    Integer minExperience,
                                                    Integer maxExperience,
                                                    Integer minLevel,
                                                    Integer maxLevel) {

        Specification<Player> resultSpecification = (playerRoot, query, builder) ->
                builder.isTrue(builder.literal(true));
        if (name != null) {
            Specification<Player> filterName = (playerRoot, query, builder) ->
                    builder.like(playerRoot.get("name"), "%" + name + "%");
            resultSpecification = resultSpecification.and(filterName);
        }
        if (title != null) {
            Specification<Player> filterTitle = (playerRoot, query, builder) ->
                    builder.like(playerRoot.get("title"), "%" + title + "%");
            resultSpecification = resultSpecification != null ? resultSpecification.and(filterTitle) :
                    (playerRoot, query, builder) -> builder.isTrue(builder.literal(true));
        }
        if (race != null) {
            Specification<Player> filterRace = (playerRoot, query, builder) ->
                    builder.equal(playerRoot.get("race"), race);
            resultSpecification = resultSpecification != null ? resultSpecification.and(filterRace) :
                    (playerRoot, query, builder) -> builder.isTrue(builder.literal(true));
        }
        if (profession != null) {
            Specification<Player> filterProfession = (playerRoot, query, builder) ->
                    builder.equal(playerRoot.get("profession"), profession);
            resultSpecification = resultSpecification != null ? resultSpecification.and(filterProfession) :
                    (playerRoot, query, builder) -> builder.isTrue(builder.literal(true));
        }
        if (after != null) {
            Specification<Player> filterAfter = (playerRoot, query, builder) ->
                    builder.greaterThanOrEqualTo(playerRoot.get("birthday"), new Date(after));
            resultSpecification = resultSpecification != null ? resultSpecification.and(filterAfter) :
                    (playerRoot, query, builder) -> builder.isTrue(builder.literal(true));
        }
        if (before != null) {
            Specification<Player> filterBefore = (playerRoot, query, builder) ->
                    builder.lessThanOrEqualTo(playerRoot.get("birthday"), new Date(before));
            resultSpecification = resultSpecification != null ? resultSpecification.and(filterBefore) :
                    (playerRoot, query, builder) -> builder.isTrue(builder.literal(true));
        }
        if (banned != null) {
            Specification<Player> filterBanned = (playerRoot, query, builder) ->
                    builder.equal(playerRoot.get("banned"), banned);
            resultSpecification = resultSpecification != null ? resultSpecification.and(filterBanned) :
                    (playerRoot, query, builder) -> builder.isTrue(builder.literal(true));
        }
        if (minExperience != null) {
            Specification<Player> filterMinExperience = (playerRoot, query, builder) ->
                    builder.ge(playerRoot.get("experience"), minExperience);
            resultSpecification = resultSpecification != null ? resultSpecification.and(filterMinExperience) :
                    (playerRoot, query, builder) -> builder.isTrue(builder.literal(true));
        }
        if (maxExperience != null) {
            Specification<Player> filterMaxExperience = (playerRoot, query, builder) ->
                    builder.le(playerRoot.get("experience"), maxExperience);
            resultSpecification = resultSpecification != null ? resultSpecification.and(filterMaxExperience) :
                    (playerRoot, query, builder) -> builder.isTrue(builder.literal(true));
        }
        if (minLevel != null) {
            Specification<Player> filterMinLevel = (playerRoot, query, builder) ->
                    builder.ge(playerRoot.get("level"), minLevel);
            resultSpecification = resultSpecification != null ? resultSpecification.and(filterMinLevel) :
                    (playerRoot, query, builder) -> builder.isTrue(builder.literal(true));
        }
        if (maxLevel != null) {
            Specification<Player> filterMaxLevel = (playerRoot, query, builder) ->
                    builder.le(playerRoot.get("level"), maxLevel);
            resultSpecification = resultSpecification != null ? resultSpecification.and(filterMaxLevel) :
                    (playerRoot, query, builder) -> builder.isTrue(builder.literal(true));
        }

        return resultSpecification;
    }

    private Boolean isIdValid(Long id) {
        if (id == null) return false;
        return (id > 0);
    }

    private Boolean isNameValid(String name) {
        if (name == null || name.isEmpty()) return false;
        return (name.length() <= 12);
    }

    private Boolean isTitleValid(String title) {
        if (title == null) return false;
        return (title.length() <= 30);
    }

    private Boolean isDateValid(Date date) {
        if (date == null) return false;
        return date.after(new GregorianCalendar(2000, Calendar.JANUARY, 1).getTime())
                && date.before(new GregorianCalendar(3000, Calendar.DECEMBER, 31).getTime());
    }

    private Boolean isExperienceValid (Integer experience) {
        if (experience == null) return false;
        return (experience >= 0 && experience <= 10000000);
    }

    private Integer calculateLevel(Integer experience) {
        return (int)((Math.sqrt(2500D + 200D * experience) - 50) / 100);
    }

    private Integer calculateUntilNextLevel(Integer experience, Integer level) {
        return 50 * (level + 1) * (level + 2) - experience;
    }
}

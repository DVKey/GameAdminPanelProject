package com.game.service;

import com.game.entity.Player;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface PlayerService {

    List<Player> getAllPlayers(Specification<Player> spec, Pageable pageable);

    Player getPlayerById(Long id);

    void save(Player player);

    void delete(Long id);

    Integer getCount(Specification<Player> spec);
}

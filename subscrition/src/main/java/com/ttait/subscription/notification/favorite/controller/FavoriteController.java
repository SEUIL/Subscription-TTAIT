package com.ttait.subscription.notification.favorite.controller;

import com.ttait.subscription.common.util.CurrentUser;
import com.ttait.subscription.notification.favorite.dto.FavoriteCreateRequest;
import com.ttait.subscription.notification.favorite.dto.FavoriteResponse;
import com.ttait.subscription.notification.favorite.service.FavoriteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public ResponseEntity<Void> add(@Valid @RequestBody FavoriteCreateRequest request) {
        favoriteService.add(CurrentUser.id(), request.announcementId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{announcementId}")
    public ResponseEntity<Void> remove(@PathVariable Long announcementId) {
        favoriteService.remove(CurrentUser.id(), announcementId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<FavoriteResponse>> list(
        @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(favoriteService.list(CurrentUser.id(), pageable));
    }

    @GetMapping("/{announcementId}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable Long announcementId) {
        return ResponseEntity.ok(favoriteService.exists(CurrentUser.id(), announcementId));
    }
}

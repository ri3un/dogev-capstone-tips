package com.gaebalhagae.course.controller;

import com.gaebalhagae.course.dto.CourseDtos;
import com.gaebalhagae.course.service.CourseService;
import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.LoginMember;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping("/api/courses/recommend")
    public ApiResponse<CourseDtos.Response> recommend(@AuthenticationPrincipal LoginMember me,
                                                      @Valid @RequestBody CourseDtos.Recommend request) {
        return ApiResponse.ok(courseService.recommend(me.id(), request));
    }

    @GetMapping("/api/courses/{courseId}")
    public ApiResponse<CourseDtos.Response> get(@AuthenticationPrincipal LoginMember me, @PathVariable Long courseId) {
        return ApiResponse.ok(courseService.get(me.id(), courseId));
    }

    @GetMapping("/api/dogs/{dogId}/courses")
    public ApiResponse<List<CourseDtos.Response>> list(@AuthenticationPrincipal LoginMember me,
                                                       @PathVariable Long dogId) {
        return ApiResponse.ok(courseService.list(me.id(), dogId));
    }

    @PostMapping("/api/courses/{courseId}/complete")
    public ApiResponse<CourseDtos.Response> complete(@AuthenticationPrincipal LoginMember me,
                                                     @PathVariable Long courseId) {
        return ApiResponse.ok(courseService.complete(me.id(), courseId));
    }

    @PostMapping("/api/courses/{courseId}/save")
    public ApiResponse<CourseDtos.Response> save(@AuthenticationPrincipal LoginMember me, @PathVariable Long courseId) {
        return ApiResponse.ok(courseService.save(me.id(), courseId));
    }

    @DeleteMapping("/api/courses/{courseId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginMember me, @PathVariable Long courseId) {
        courseService.delete(me.id(), courseId);
        return ApiResponse.ok();
    }
}

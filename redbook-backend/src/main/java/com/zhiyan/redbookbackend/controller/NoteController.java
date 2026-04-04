package com.zhiyan.redbookbackend.controller;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.req.NoteSaveDTO;
import com.zhiyan.redbookbackend.service.INoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Tag(name = "笔记")
@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
public class NoteController {

    private final INoteService noteService;

    @Operation(summary = "保存或发布笔记")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "根节点必须是 JSON 对象（以 { 开头），不要整段用引号包成字符串；长正文里的换行请写成 \\n。",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(
                            name = "图文发布",
                            value = """
                                    {
                                      "id": null,
                                      "title": "示例标题",
                                      "content": "第一段\\n第二段",
                                      "type": 0,
                                      "visibility": 0,
                                      "mediaUrls": [],
                                      "productIds": [],
                                      "publish": true
                                    }
                                    """)))
    @PostMapping("/save")
    public Result save(@RequestBody NoteSaveDTO dto) {
        return noteService.save(dto);
    }

    @Operation(summary = "将草稿发布")
    @PostMapping("/publish/{id}")
    public Result publish(@PathVariable("id") Long id) {
        return noteService.publish(id);
    }

    @Operation(summary = "删除笔记")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable("id") Long id) {
        return noteService.delete(id);
    }

    @Operation(summary = "笔记详情（公开）")
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable("id") Long id) {
        return noteService.detail(id);
    }

    @Operation(summary = "我的笔记分页")
    @GetMapping("/my")
    public Result my(@RequestParam(defaultValue = "1") long current,
                     @RequestParam(defaultValue = "10") long size) {
        return noteService.myNotes(current, size);
    }

    @Operation(summary = "关注动态（时间游标分页）")
    @GetMapping("/feed")
    public Result feed(@RequestParam(required = false) Long maxTime,
                       @RequestParam(required = false) Integer offset,
                       @RequestParam(defaultValue = "10") int size) {
        return noteService.feed(maxTime, offset, size);
    }

    @Operation(summary = "推荐流（公开）")
    @GetMapping("/recommend")
    public Result recommend(@RequestParam(defaultValue = "1") long current,
                            @RequestParam(defaultValue = "10") long size) {
        return noteService.recommend(current, size);
    }

    @Operation(summary = "相关笔记")
    @GetMapping("/related/{id}")
    public Result related(@PathVariable("id") Long id) {
        return noteService.related(id);
    }
}

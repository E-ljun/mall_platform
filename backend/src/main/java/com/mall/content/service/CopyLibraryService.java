package com.mall.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.content.domain.entity.CopyLibrary;
import com.mall.content.domain.entity.MarketingCopy;
import com.mall.content.mapper.CopyLibraryMapper;
import com.mall.content.mapper.MarketingCopyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CopyLibraryService {

    private final CopyLibraryMapper copyLibraryMapper;
    private final MarketingCopyMapper marketingCopyMapper;
    private final com.mall.content.mapper.ProductMapper productMapper;

    public Page<CopyLibrary> list(Long userId, int page, int size, String platform, String keyword, String groupName) {
        LambdaQueryWrapper<CopyLibrary> w = new LambdaQueryWrapper<CopyLibrary>()
                .eq(CopyLibrary::getUserId, userId)
                .ne(CopyLibrary::getContent, ""); // 排除空内容的分组标记条目
        if (platform != null && !platform.isBlank()) {
            w.eq(CopyLibrary::getPlatform, platform);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(CopyLibrary::getTitle, keyword).or().like(CopyLibrary::getContent, keyword));
        }
        if (groupName != null && !groupName.isBlank()) {
            w.eq(CopyLibrary::getGroupName, groupName);
        }
        w.orderByDesc(CopyLibrary::getCreatedAt);
        return copyLibraryMapper.selectPage(new Page<>(page, size), w);
    }

    public List<String> listGroups(Long userId) {
        return copyLibraryMapper.selectList(
            new LambdaQueryWrapper<CopyLibrary>()
                .select(CopyLibrary::getGroupName)
                .eq(CopyLibrary::getUserId, userId)
                .groupBy(CopyLibrary::getGroupName)
                .orderByAsc(CopyLibrary::getGroupName)
        ).stream().map(CopyLibrary::getGroupName).distinct().toList();
    }

    @Transactional
    public CopyLibrary collectFromCopy(Long copyId, String groupName, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        MarketingCopy copy = marketingCopyMapper.selectById(copyId);
        if (copy == null) throw new IllegalArgumentException("文案不存在");

        CopyLibrary lib = new CopyLibrary();
        lib.setUserId(userId);
        lib.setTitle(copy.getTitle());
        lib.setPlatform(copy.getPlatform());
        lib.setContent(copy.getContent());
        lib.setTags(copy.getHashtags());
        lib.setGroupName(groupName != null && !groupName.isBlank() ? groupName : "默认");
        // 附带来源商品信息
        var product = productMapper.selectById(copy.getProductId());
        if (product != null) {
            lib.setProductName(product.getName());
            lib.setProductImage(product.getMainImageUrl());
        }
        copyLibraryMapper.insert(lib);

        // 确保分组有锚点标记条目（防止所有条目移出后分组消失）
        if (lib.getGroupName() != null && !lib.getGroupName().isBlank() && !"默认".equals(lib.getGroupName())) {
            long markerCount = copyLibraryMapper.selectCount(
                new LambdaQueryWrapper<CopyLibrary>()
                    .eq(CopyLibrary::getUserId, userId)
                    .eq(CopyLibrary::getGroupName, lib.getGroupName())
                    .eq(CopyLibrary::getContent, "")
            );
            if (markerCount == 0) {
                CopyLibrary marker = new CopyLibrary();
                marker.setUserId(userId);
                marker.setTitle(lib.getGroupName());
                marker.setPlatform("OTHER");
                marker.setContent("");
                marker.setGroupName(lib.getGroupName());
                copyLibraryMapper.insert(marker);
            }
        }

        return lib;
    }

    @Transactional
    public CopyLibrary update(Long id, String title, String content, String groupName, Authentication auth) {
        CopyLibrary lib = copyLibraryMapper.selectById(id);
        if (lib == null) throw new IllegalArgumentException("文库条目不存在");
        if (!lib.getUserId().equals((Long) auth.getPrincipal()))
            throw new IllegalStateException("无权操作");
        if (title != null) lib.setTitle(title);
        if (content != null) lib.setContent(content);
        if (groupName != null) lib.setGroupName(groupName);
        copyLibraryMapper.updateById(lib);
        return lib;
    }

    @Transactional
    public void moveGroup(List<Long> ids, String groupName, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        for (Long id : ids) {
            CopyLibrary lib = copyLibraryMapper.selectById(id);
            if (lib != null && lib.getUserId().equals(userId)) {
                lib.setGroupName(groupName != null && !groupName.isBlank() ? groupName : "默认");
                copyLibraryMapper.updateById(lib);
            }
        }
    }

    public CopyLibrary getById(Long id, Authentication auth) {
        CopyLibrary lib = copyLibraryMapper.selectById(id);
        if (lib == null) throw new IllegalArgumentException("文库条目不存在");
        if (!lib.getUserId().equals((Long) auth.getPrincipal()))
            throw new IllegalStateException("无权访问");
        return lib;
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        CopyLibrary lib = copyLibraryMapper.selectById(id);
        if (lib == null) throw new IllegalArgumentException("文库条目不存在");
        if (!lib.getUserId().equals((Long) auth.getPrincipal()))
            throw new IllegalStateException("无权操作");
        copyLibraryMapper.deleteById(id);
    }

    // ==================== 分组管理 ====================

    @Transactional
    public void createGroup(Long userId, String groupName) {
        if (groupName == null || groupName.isBlank()) throw new IllegalArgumentException("分组名不能为空");
        if ("默认".equals(groupName)) throw new IllegalArgumentException("默认分组始终存在，无需创建");

        // 检查是否已有该分组
        long count = copyLibraryMapper.selectCount(
            new LambdaQueryWrapper<CopyLibrary>()
                .eq(CopyLibrary::getUserId, userId)
                .eq(CopyLibrary::getGroupName, groupName)
        );
        if (count > 0) throw new IllegalArgumentException("分组「" + groupName + "」已存在");

        // 插入一条标记条目来锚定分组（内容为空，列表中会被过滤）
        CopyLibrary marker = new CopyLibrary();
        marker.setUserId(userId);
        marker.setTitle(groupName);
        marker.setPlatform("OTHER");
        marker.setContent("");
        marker.setGroupName(groupName);
        copyLibraryMapper.insert(marker);
    }

    @Transactional
    public void renameGroup(Long userId, String oldName, String newName) {
        if (oldName == null || oldName.isBlank()) throw new IllegalArgumentException("原分组名不能为空");
        if (newName == null || newName.isBlank()) throw new IllegalArgumentException("新分组名不能为空");
        if (oldName.equals(newName)) return;

        // 检查新分组名是否已存在
        long count = copyLibraryMapper.selectCount(
            new LambdaQueryWrapper<CopyLibrary>()
                .eq(CopyLibrary::getUserId, userId)
                .eq(CopyLibrary::getGroupName, newName)
        );
        if (count > 0) throw new IllegalArgumentException("分组名「" + newName + "」已存在");

        // 更新所有该分组下的条目
        List<CopyLibrary> entries = copyLibraryMapper.selectList(
            new LambdaQueryWrapper<CopyLibrary>()
                .eq(CopyLibrary::getUserId, userId)
                .eq(CopyLibrary::getGroupName, oldName)
        );
        for (CopyLibrary entry : entries) {
            entry.setGroupName(newName);
            copyLibraryMapper.updateById(entry);
        }
    }

    @Transactional
    public int deleteGroup(Long userId, String groupName) {
        if (groupName == null || groupName.isBlank()) throw new IllegalArgumentException("分组名不能为空");
        if ("默认".equals(groupName)) throw new IllegalArgumentException("不能删除默认分组");

        List<CopyLibrary> entries = copyLibraryMapper.selectList(
            new LambdaQueryWrapper<CopyLibrary>()
                .eq(CopyLibrary::getUserId, userId)
                .eq(CopyLibrary::getGroupName, groupName)
        );
        if (entries.isEmpty()) throw new IllegalArgumentException("分组「" + groupName + "」不存在或已为空");

        for (CopyLibrary entry : entries) {
            copyLibraryMapper.deleteById(entry.getId());
        }
        return entries.size();
    }
}

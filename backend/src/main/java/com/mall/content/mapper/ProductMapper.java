package com.mall.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.content.domain.entity.Product;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /** 绕过 @TableLogic 查询已删除商品（分页） */
    @Select("SELECT * FROM product WHERE deleted = 1 AND user_id = #{userId} ORDER BY updated_at DESC")
    Page<Product> selectDeletedByUserId(Page<Product> page, @Param("userId") Long userId);

    /** 管理员：查看所有已删除商品（分页） */
    @Select("SELECT * FROM product WHERE deleted = 1 ORDER BY updated_at DESC")
    Page<Product> selectAllDeleted(Page<Product> page);

    /** 绕过 @TableLogic 根据 ID 查询已删除商品 */
    @Select("SELECT * FROM product WHERE id = #{id} AND deleted = 1")
    Product selectDeletedById(@Param("id") Long id);

    /** 物理删除（绕过 @TableLogic） */
    @Delete("DELETE FROM product WHERE id = #{id} AND deleted = 1")
    int deletePhysically(@Param("id") Long id);

    /** 恢复已删除商品（绕过 @TableLogic 的 UPDATE 过滤） */
    @org.apache.ibatis.annotations.Update("UPDATE product SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreDeleted(@Param("id") Long id);
}

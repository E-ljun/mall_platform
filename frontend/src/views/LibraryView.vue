<template>
  <div class="library-page">
    <div class="page-hero">
      <div class="hero-content">
        <h2>📚 文案库</h2>
        <p class="hero-sub">收藏优质文案为模板，跨商品复用</p>
      </div>
    </div>

    <!-- Group tabs -->
    <div class="group-bar">
      <div class="group-tabs">
        <button
          :class="['group-tab', { active: activeGroup === '' }]"
          @click="switchGroup('')"
        >全部</button>
        <div
          v-for="g in groups"
          :key="g"
          class="group-tab-wrapper"
        >
          <button
            :class="['group-tab', { active: activeGroup === g }]"
            @click="switchGroup(g)"
          >
            <span class="group-folder">📁</span>
            <span>{{ g }}</span>
          </button>
          <el-dropdown trigger="click" @command="(cmd: string) => onGroupAction(cmd, g)">
            <button class="group-tab-menu" @click.stop title="分组操作">
              <span class="group-menu-dot">⋮</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">✏️ 重命名</el-dropdown-item>
                <el-dropdown-item command="delete" divided style="color:var(--el-color-danger);">🗑️ 删除分组</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <div class="group-actions">
        <el-button size="small" @click="showAddGroup"><el-icon><Plus /></el-icon> 新分组</el-button>
        <el-dropdown v-if="selectedIds.length > 0" @command="onMoveItems">
          <el-button size="small" type="primary" plain>移动到…</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="g in groups" :key="g" :command="g">📁 {{ g }}</el-dropdown-item>
              <el-dropdown-item divided command="默认">📁 默认</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="平台">
          <el-select v-model="filters.platform" clearable placeholder="全部" style="width:120px">
            <el-option label="小红书" value="XIAOHONGSHU" />
            <el-option label="淘宝" value="TAOBAO" />
            <el-option label="抖音" value="DOUYIN" />
            <el-option label="其它" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="标题/内容" clearable style="width:180px" />
        </el-form-item>
        <el-button type="primary" @click="load">搜索</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </el-form>
    </el-card>

    <div v-if="!loading && items.length > 1" class="batch-bar">
      <el-dropdown @command="exportBatch">
        <el-button type="primary" plain size="small">批量导出 ({{ items.length }})</el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="txt">TXT</el-dropdown-item>
            <el-dropdown-item command="xlsx">Excel</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-row :gutter="20" v-loading="loading">
      <el-col v-for="item in items" :key="item.id" :xs="24" :md="12" :lg="8">
        <el-card shadow="hover" class="lib-card" :class="{ selected: selectedIds.includes(item.id) }">
          <div class="card-head">
            <div class="card-head-left">
              <el-tag size="small">{{ platformLabel(item.platform) }}</el-tag>
              <span class="card-group-tag" v-if="item.groupName && item.groupName !== '默认'">📁 {{ item.groupName }}</span>
            </div>
            <div>
              <el-checkbox
                :model-value="selectedIds.includes(item.id)"
                @change="(v: boolean) => toggleSelect(item.id, v)"
                style="margin-right:6px"
              />
              <el-dropdown @command="(fmt: string) => exportOne(item.id, fmt)">
                <el-button link size="small" type="primary">导出</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="txt">TXT</el-dropdown-item>
                    <el-dropdown-item command="md">MD</el-dropdown-item>
                    <el-dropdown-item command="pdf">PDF</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button link size="small" type="danger" @click="removeItem(item.id)">删除</el-button>
            </div>
          </div>
          <div class="product-row" v-if="item.productImage || item.productName">
            <el-image v-if="item.productImage" :src="item.productImage" fit="cover" class="product-thumb" />
            <span v-if="item.productName" class="product-name">{{ item.productName }}</span>
          </div>
          <h4 class="card-title">{{ item.title || '无标题' }}</h4>
          <p class="card-content">{{ item.content?.slice(0, 120) }}{{ item.content?.length > 120 ? '...' : '' }}</p>
          <div class="card-tags" v-if="item.tags">
            <el-tag v-for="tag in parseTags(item.tags)" :key="tag" size="small" type="info" class="tag">{{ tag }}</el-tag>
          </div>
          <div class="card-foot">
            <el-button size="small" type="primary" plain @click="editItem(item)">编辑</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && items.length === 0" description="暂无收藏，在商品编辑页将文案收藏到文库" />

    <el-pagination
      v-if="total > 0" v-model:current-page="page" :page-size="size" :total="total"
      layout="total, prev, pager, next" class="pager" @current-change="load"
    />

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑文库文案" width="500px">
      <el-form label-width="60px">
        <el-form-item label="分组">
          <el-select v-model="editForm.groupName" filterable allow-create placeholder="默认">
            <el-option v-for="g in groups" :key="g" :label="g" :value="g" />
            <el-option label="默认" value="默认" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="editForm.content" type="textarea" :rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增分组弹窗 -->
    <el-dialog v-model="addGroupVisible" title="新建分组" width="380px">
      <p style="font-size:13px;color:var(--text-muted);margin:0 0 12px;">
        输入分组名称后点击创建，分组将持久保存。
      </p>
      <el-input v-model="newGroupName" placeholder="输入分组名称" maxlength="20" @keyup.enter="createGroup" />
      <template #footer>
        <el-button @click="addGroupVisible = false">取消</el-button>
        <el-button type="primary" @click="createGroup" :loading="groupSaving">创建</el-button>
      </template>
    </el-dialog>

    <!-- 重命名分组弹窗 -->
    <el-dialog v-model="renameVisible" title="重命名分组" width="380px">
      <el-input v-model="renameNewName" placeholder="输入新名称" maxlength="20" @keyup.enter="doRenameGroup" />
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="doRenameGroup" :loading="groupSaving">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import http, { type ApiResponse } from '../api/http'

interface LibItem { id: number; title: string; platform: string; productName: string | null; productImage: string | null; content: string; tags: string | null; groupName: string | null }

const items = ref<LibItem[]>([])
const loading = ref(false)
const page = ref(1); const size = 12; const total = ref(0)
const filters = reactive({ platform: '', keyword: '' })

// Group state
const groups = ref<string[]>([])
const activeGroup = ref('')
const selectedIds = ref<number[]>([])
const addGroupVisible = ref(false)
const newGroupName = ref('')
const groupSaving = ref(false)

// Rename state
const renameVisible = ref(false)
const renameOldName = ref('')
const renameNewName = ref('')

const editVisible = ref(false)
const editForm = ref({ id: 0, title: '', content: '', groupName: '' })

function platformLabel(p: string) {
  return { XIAOHONGSHU: '小红书', TAOBAO: '淘宝', DOUYIN: '抖音', OTHER: '其它' }[p] || p
}
function parseTags(raw: string | null): string[] {
  if (!raw) return []
  try { return JSON.parse(raw) } catch { return raw.split(',').filter(Boolean) }
}

function toggleSelect(id: number, v: boolean) {
  if (v) selectedIds.value.push(id)
  else selectedIds.value = selectedIds.value.filter(x => x !== id)
}

async function loadGroups() {
  try {
    const { data } = await http.get<ApiResponse<string[]>>('/library/groups')
    groups.value = (data.data || []).filter(g => g !== '默认')
  } catch { /* ignore */ }
}

function switchGroup(groupName: string) {
  activeGroup.value = groupName
  page.value = 1
  selectedIds.value = []
  load()
}

async function load() {
  loading.value = true
  try {
    const { data } = await http.get<ApiResponse<any>>('/library', {
      params: {
        page: page.value, size,
        platform: filters.platform || undefined,
        keyword: filters.keyword || undefined,
        groupName: activeGroup.value || undefined,
      }
    })
    items.value = data.data.records
    total.value = data.data.total
  } finally { loading.value = false }
}
function resetFilters() { filters.platform = ''; filters.keyword = ''; page.value = 1; load() }

function showAddGroup() { newGroupName.value = ''; addGroupVisible.value = true }

async function createGroup() {
  const name = newGroupName.value.trim()
  if (!name) return
  if (groups.value.includes(name)) {
    ElMessage.warning('分组名已存在'); return
  }
  groupSaving.value = true
  try {
    await http.post('/library/groups', { groupName: name })
    groups.value.push(name)
    addGroupVisible.value = false
    ElMessage.success(`分组「${name}」已创建`)
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.error?.message || '创建失败')
  } finally {
    groupSaving.value = false
  }
}

// 分组操作：重命名 / 删除
function onGroupAction(cmd: string, groupName: string) {
  if (cmd === 'rename') {
    renameOldName.value = groupName
    renameNewName.value = ''
    renameVisible.value = true
  } else if (cmd === 'delete') {
    deleteGroup(groupName)
  }
}

async function doRenameGroup() {
  const newName = renameNewName.value.trim()
  if (!newName) return
  if (newName === renameOldName.value) { renameVisible.value = false; return }
  if (groups.value.includes(newName)) {
    ElMessage.warning('分组名已存在'); return
  }
  groupSaving.value = true
  try {
    await http.put('/library/groups/rename', { oldName: renameOldName.value, newName })
    ElMessage.success(`已重命名为「${newName}」`)
    renameVisible.value = false
    if (activeGroup.value === renameOldName.value) {
      activeGroup.value = newName
    }
    loadGroups()
    load()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.error?.message || '重命名失败')
  } finally {
    groupSaving.value = false
  }
}

async function deleteGroup(groupName: string) {
  if (groupName === '默认') {
    ElMessage.warning('不能删除默认分组')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除分组「${groupName}」及其所有文案？此操作不可恢复。`,
      '删除分组',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch { return }
  groupSaving.value = true
  try {
    await http.delete(`/library/groups/${encodeURIComponent(groupName)}`)
    ElMessage.success(`已删除分组「${groupName}」`)
    if (activeGroup.value === groupName) {
      activeGroup.value = ''
    }
    loadGroups()
    load()
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.error?.message || '删除失败')
  } finally {
    groupSaving.value = false
  }
}

async function onMoveItems(targetGroup: string) {
  if (!selectedIds.value.length) return
  await http.put('/library/move-group', { ids: selectedIds.value, groupName: targetGroup })
  ElMessage.success(`已移动到「${targetGroup}」`)
  selectedIds.value = []
  loadGroups()
  load()
}

async function removeItem(id: number) {
  await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
  await http.delete(`/library/${id}`)
  ElMessage.success('已删除')
  loadGroups()
  load()
}

function editItem(item: LibItem) {
  editForm.value = { id: item.id, title: item.title, content: item.content, groupName: item.groupName || '默认' }
  editVisible.value = true
}
async function saveEdit() {
  await http.put(`/library/${editForm.value.id}`, {
    title: editForm.value.title,
    content: editForm.value.content,
    groupName: editForm.value.groupName,
  })
  ElMessage.success('已保存')
  editVisible.value = false
  loadGroups()
  load()
}

async function exportOne(id: number, format: string) {
  const res = await http.get(`/library/${id}/export`, { params: { format }, responseType: 'blob' })
  const blob = new Blob([res.data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = `library-${id}.${format === 'md' ? 'md' : format === 'pdf' ? 'pdf' : 'txt'}`
  a.click(); URL.revokeObjectURL(url)
}
async function exportBatch(format: string) {
  const ids = items.value.map(i => i.id)
  if (!ids.length) return
  const res = await http.post('/library/export-batch', { ids, format }, { responseType: 'blob' })
  const blob = new Blob([res.data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = format === 'xlsx' ? 'library-batch.xlsx' : 'library-batch.txt'
  a.click(); URL.revokeObjectURL(url)
  ElMessage.success('批量导出完成')
}

onMounted(() => { loadGroups(); load() })
</script>

<style scoped>
.library-page { padding: 8px; max-width: 1200px; }
.page-hero {
  background: linear-gradient(135deg, rgba(91,138,138,0.18), rgba(125,171,171,0.12));
  border: 1px solid rgba(91,138,138,0.12);
  border-radius: var(--radius-lg); padding: 28px 36px; margin-bottom: 20px;
}
.hero-content h2 { margin: 0 0 6px; color: var(--text-primary); font-size: 22px; font-weight: 700; font-family: var(--font-display); }
.hero-sub { margin: 0; color: var(--text-secondary); font-size: 14px; }

/* Group bar */
.group-bar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px; gap: 12px;
  padding: 10px 16px;
  background: var(--bg-surface);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
}
.group-tabs { display: flex; gap: 2px; flex-wrap: wrap; overflow-x: auto; flex: 1; align-items: center; }
.group-tab-wrapper {
  display: flex; align-items: center;
  border-radius: 20px;
  transition: all 0.25s var(--ease-out);
}
.group-tab-wrapper:hover {
  background: rgba(0,0,0,0.03);
}
.group-tab {
  padding: 6px 4px 6px 14px; border-radius: 20px 0 0 20px;
  border: 1px solid transparent; border-right: none;
  background: transparent; color: var(--text-secondary);
  font-size: 13px; cursor: pointer; white-space: nowrap;
  transition: all 0.25s var(--ease-out);
  display: flex; align-items: center; gap: 4px;
}
.group-tab:hover { color: var(--text-primary); }
.group-tab.active {
  color: var(--accent); background: rgba(212,168,83,0.08);
  border-color: rgba(212,168,83,0.2);
}
.group-tab-menu {
  padding: 6px 10px 6px 4px; border-radius: 0 20px 20px 0;
  border: 1px solid transparent; border-left: none;
  background: transparent; color: var(--text-muted);
  font-size: 14px; cursor: pointer;
  transition: all 0.25s var(--ease-out);
  line-height: 1;
}
.group-tab-menu:hover { color: var(--text-primary); }
.group-tab-wrapper:has(.group-tab.active) .group-tab-menu {
  border-color: rgba(212,168,83,0.2);
  background: rgba(212,168,83,0.08);
}
.group-menu-dot { font-weight: 700; letter-spacing: 1px; }
.group-folder { font-size: 14px; }
.group-actions { display: flex; gap: 8px; flex-shrink: 0; }

.filter-card { margin-bottom: 16px; border-radius: var(--radius-md); }
.batch-bar { margin-bottom: 12px; }
.lib-card {
  margin-bottom: 20px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
  background: var(--bg-surface);
  transition: all 0.3s var(--ease-out);
}
.lib-card:hover {
  border-color: var(--accent-teal);
  box-shadow: 0 8px 32px rgba(0,0,0,0.08);
}
.lib-card.selected {
  border-color: var(--accent);
  box-shadow: 0 0 0 1px rgba(196,155,74,0.2), 0 4px 20px rgba(196,155,74,0.08);
}
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.card-head-left { display: flex; align-items: center; gap: 6px; }
.card-group-tag {
  font-size: 11px; color: var(--accent-teal-light);
  background: rgba(91,138,138,0.08);
  padding: 2px 8px; border-radius: 4px;
}
.product-row {
  display: flex; align-items: center; gap: 10px; margin-bottom: 10px;
  padding: 8px; background: rgba(0,0,0,0.02); border-radius: 8px;
  border: 1px solid var(--border-subtle);
}
.product-thumb { width: 48px; height: 48px; border-radius: 6px; flex-shrink: 0; }
.product-name { font-size: 13px; color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-title { margin: 0 0 8px; font-size: 15px; font-weight: 600; color: var(--text-primary); }
.card-content { color: var(--text-secondary); font-size: 13px; line-height: 1.6; margin-bottom: 8px; }
.card-tags { display: flex; gap: 4px; flex-wrap: wrap; margin-bottom: 10px; }
.tag { font-size: 11px; }
.card-foot { text-align: right; }
.pager { margin-top: 20px; justify-content: center; }
</style>

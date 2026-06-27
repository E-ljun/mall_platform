<template>
  <div class="page" v-loading="pageLoading">
    <el-page-header @back="$router.push('/products')" content="商品编辑" />

    <el-row :gutter="20" v-if="product">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>基础信息</template>
          <el-form label-width="90px">
            <el-form-item label="名称"><el-input v-model="product.name" /></el-form-item>
            <el-form-item label="价格"><el-input-number v-model="product.price" :min="0" style="width:100%" /></el-form-item>
            <el-form-item label="分类">
              <el-select v-model="product.categoryId" clearable placeholder="选择分类" style="width:100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="库存"><el-input-number v-model="product.stock" :min="0" style="width:100%" /></el-form-item>
            <el-form-item label="状态">
              <el-select v-model="product.status" style="width:100%">
                <el-option label="草稿（未上架）" value="DRAFT" />
                <el-option label="上架（对外可见）" value="ON_SHELF" />
                <el-option label="下架（已隐藏）" value="OFF_SHELF" />
              </el-select>
              <span style="font-size:12px;color:#999;">用于管理商品是否对外展示</span>
            </el-form-item>
            <el-form-item label="关键词"><el-input v-model="keywords" placeholder="补充关键词，如：纯棉、夏季、透气" /></el-form-item>
            <el-form-item label="精炼标题">
              <el-input v-model="product.shortTitle" maxlength="15" show-word-limit />
            </el-form-item>
            <el-form-item label="核心卖点">
              <div class="selling-points">
                <div v-for="(p, i) in sellingPoints" :key="i" class="sp-row">
                  <el-input v-model="sellingPoints[i]" />
                  <el-button link type="danger" @click="sellingPoints.splice(i, 1)">删</el-button>
                </div>
                <el-button v-if="sellingPoints.length < 5" @click="sellingPoints.push('')">+ 添加卖点</el-button>
              </div>
            </el-form-item>
            <el-form-item label="详情描述">
              <el-input v-model="product.detailContent" type="textarea" :rows="5" maxlength="300" show-word-limit />
            </el-form-item>
            <el-button type="primary" @click="saveProduct">保存商品</el-button>
          </el-form>
        </el-card>

        <el-card class="mt">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <span>商品图片（拖拽排序，首张为主图，最多10张）</span>
              <el-dropdown v-if="images.length > 0" @command="exportImages">
                <el-button size="small" type="primary" plain>导出图片</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="all">全部导出 (ZIP)</el-dropdown-item>
                    <el-dropdown-item command="selected">导出选中</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
          <el-upload
            drag multiple
            :auto-upload="false"
            :show-file-list="false"
            accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
            :on-change="onFileChange"
            :disabled="images.length >= 10"
          >
            <div>拖拽或点击上传 JPG/PNG/WebP，单张≤5MB</div>
          </el-upload>

          <draggable v-model="images" item-key="id" class="image-grid" @end="onReorder">
            <template #item="{ element }">
              <div class="img-card">
                <el-image :src="element.url" fit="cover" class="thumb" :preview-src-list="previewList" />
                <el-checkbox v-model="element.selected" @change="syncSelected">用于AI</el-checkbox>
                <el-tag v-if="element.isMain" size="small" type="success">主图</el-tag>
                <el-button link type="danger" size="small" @click="removeImage(element.id)">删除</el-button>
                <el-button link type="primary" size="small" @click="downloadImage(element.id)">下载</el-button>
              </div>
            </template>
          </draggable>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <!-- ======== 一键组合生成（核心） ======== -->
        <el-card class="pipeline-card">
          <template #header>
            <span style="font-weight:700;">🚀 一键组合生成</span>
            <el-tag size="small" type="warning" style="margin-left:8px;">先选步骤再生成</el-tag>
          </template>

          <div class="pipeline-steps">
            <el-checkbox-group v-model="pipelineSteps">
              <div class="step-row" :class="{ active: pipelineSteps.includes('description') }">
                <el-checkbox label="description" :disabled="pipelineRunning" />
                <span class="step-label">① 商品描述</span>
                <span class="step-hint">分析图片→短标题+卖点+详情</span>
                <el-tag v-if="pipelineResults.description" type="success" size="small">✓</el-tag>
              </div>
              <div class="step-row" :class="{ active: pipelineSteps.includes('copy') }">
                <el-checkbox label="copy" :disabled="pipelineRunning" />
                <span class="step-label">② 营销文案</span>
                <span class="step-hint">基于描述生成平台文案</span>
                <el-tag v-if="pipelineResults.copy" type="success" size="small">✓</el-tag>
              </div>
              <div class="step-row" :class="{ active: pipelineSteps.includes('image') }">
                <el-checkbox label="image" :disabled="pipelineRunning" />
                <span class="step-label">③ 详情图</span>
                <span class="step-hint">Wan 2.7 生成电商模块图</span>
                <el-tag v-if="pipelineResults.image" type="success" size="small">✓</el-tag>
              </div>
            </el-checkbox-group>
          </div>

          <!-- 文案参数 -->
          <div v-if="pipelineSteps.includes('copy')" class="pipeline-params">
            <span style="margin-right:8px;">文案平台：</span>
            <el-radio-group v-model="pipelinePlatform" size="small">
              <el-radio-button label="XIAOHONGSHU">小红书</el-radio-button>
              <el-radio-button label="TAOBAO">淘宝</el-radio-button>
              <el-radio-button label="DOUYIN">抖音</el-radio-button>
              <el-radio-button label="OTHER">其它</el-radio-button>
            </el-radio-group>
          </div>

          <el-button
            type="primary"
            size="large"
            :loading="pipelineRunning"
            :disabled="pipelineSteps.length === 0"
            @click="runPipeline"
            class="pipeline-btn"
          >
            {{ pipelineRunning ? '生成中...' : '一键生成（' + pipelineSteps.length + '步）' }}
          </el-button>

          <!-- 进度指示 -->
          <div v-if="pipelineRunning" class="pipeline-progress">
            <el-steps :active="pipelineStepIndex" finish-status="success" align-center>
              <el-step v-if="pipelineSteps.includes('description')" title="描述" />
              <el-step v-if="pipelineSteps.includes('copy')" title="文案" />
              <el-step v-if="pipelineSteps.includes('image')" title="详情图" />
            </el-steps>
            <p class="progress-msg">{{ pipelineMsg }}</p>
          </div>
        </el-card>

        <!-- ======== 单独使用 ======== -->
        <el-collapse v-model="activePanels" class="mt">
          <!-- 商品描述 -->
          <el-collapse-item title="📝 AI 商品描述生成（单独）" name="desc">
            <el-alert v-if="complianceWarnings.length" type="warning" :closable="false" class="mb">
              <div v-for="(w, i) in complianceWarnings" :key="i">{{ w }}</div>
            </el-alert>
            <el-button type="success" :loading="descLoading" @click="generateDescription">生成商品描述</el-button>
            <el-button v-if="descError" @click="generateDescription">重试</el-button>
            <div v-if="product?.shortTitle" class="result-preview mt">
              <p><strong>短标题：</strong>{{ product?.shortTitle }}</p>
              <p><strong>卖点：</strong>{{ sellingPoints.join(' / ') }}</p>
            </div>
          </el-collapse-item>

          <!-- 营销文案 -->
          <el-collapse-item title="📢 多平台营销文案（单独，最多3条）" name="copy">
            <div style="display:flex;align-items:center;flex-wrap:wrap;gap:8px;margin-bottom:8px;">
              <el-radio-group v-model="platform" size="small">
                <el-radio-button label="XIAOHONGSHU">小红书</el-radio-button>
                <el-radio-button label="TAOBAO">淘宝</el-radio-button>
                <el-radio-button label="DOUYIN">抖音</el-radio-button>
                <el-radio-button label="OTHER">其它</el-radio-button>
              </el-radio-group>
              <el-select v-model="scenario" size="small" clearable placeholder="营销场景（可选）" style="width:150px;">
                <el-option label="618大促" value="618" />
                <el-option label="双11狂欢" value="double11" />
                <el-option label="新品首发" value="new_launch" />
                <el-option label="清仓特卖" value="clearance" />
              </el-select>
            </div>
            <el-button type="primary" class="ml" :loading="copyLoading" @click="generateCopy(false)">生成</el-button>
            <el-button :loading="copyLoading" @click="generateCopy(true)">换一换</el-button>
            <span v-if="copies.length > 0" style="font-size:12px;color:#999;margin-left:8px;">已用 {{ copies.length }}/3</span>
            <div v-for="copy in copies" :key="copy.id" class="copy-item">
              <div class="copy-head">
                <el-tag size="small">{{ platformLabel(copy.platform) }}</el-tag>
                <el-tag size="small" type="info">v{{ copy.variantNo }}</el-tag>
                <el-button link :type="copy.isFavorite ? 'warning' : 'default'" @click="toggleFavorite(copy)">
                  {{ copy.isFavorite ? '★' : '☆' }}
                </el-button>
                <el-dropdown @command="(fmt: string) => exportCopy(copy.id, fmt)">
                  <el-button link type="primary">导出</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="txt">TXT</el-dropdown-item>
                      <el-dropdown-item command="md">MD</el-dropdown-item>
                      <el-dropdown-item command="pdf">PDF</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button link type="danger" size="small" @click="deleteCopyItem(copy.id)">删除</el-button>
                <el-button link type="success" size="small" @click="collectToLibrary(copy.id)">收藏到文库</el-button>
              </div>
              <el-input v-model="copy.title" placeholder="标题" size="small" class="mb" />
              <el-input v-model="copy.content" type="textarea" :rows="4" size="small" />
              <el-button size="small" class="mt" @click="saveCopy(copy)">保存</el-button>
            </div>
            <div v-if="copies.length > 1" class="mt" style="border-top:1px solid #eee;padding-top:12px;">
              <el-dropdown @command="exportBatch">
                <el-button type="primary" plain size="small">批量导出 ({{ copies.length }})</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="txt">TXT</el-dropdown-item>
                    <el-dropdown-item command="xlsx">Excel</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <el-empty v-if="copies.length === 0" description="暂无文案" :image-size="40" />
          </el-collapse-item>

          <!-- 详情图 -->
          <el-collapse-item title="🎨 AI 详情图生成 Wan 2.7（单独）" name="image">
            <p style="font-size:12px;color:#999;margin:0 0 8px;">
              不会填？查看 <router-link to="/guide" style="color:#6c5ce7;font-weight:600;">📖 AI 出图指南</router-link>，六大风格模板可直接复制
            </p>
            <el-form label-width="80px" size="small">
              <el-form-item label="模块标题">
                <el-input v-model="imgGen.sectionTitle" placeholder="如：核心参数对比" />
              </el-form-item>
              <el-form-item label="文案要点">
                <el-input v-model="imgGen.sectionCopy" type="textarea" :rows="2" placeholder="图中需包含的信息" />
              </el-form-item>
              <el-form-item label="画面描述">
                <el-input v-model="imgGen.visualDirection" type="textarea" :rows="3" placeholder="如：科技感蓝黑背景，左侧参数表" />
              </el-form-item>
              <el-form-item label="比例">
                <el-select v-model="imgGen.aspectRatio" style="width:100px">
                  <el-option label="3:4" value="3:4" />
                  <el-option label="1:1" value="1:1" />
                  <el-option label="16:9" value="16:9" />
                  <el-option label="9:16" value="9:16" />
                </el-select>
              </el-form-item>
              <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
                <el-button type="primary" :loading="imgLoading" @click="generateDetailImage">生成</el-button>
                <el-button v-if="imgError" @click="generateDetailImage">重试</el-button>
                <el-button type="success" plain :loading="imgFillLoading" @click="autoFillDetailImage">
                  ✨ 一键填写
                </el-button>
                <el-button @click="saveImgGenDraft" :loading="imgDraftSaving">💾 暂存草稿</el-button>
                <span v-if="imgDraftSaved" style="font-size:12px;color:var(--success);">已暂存 ✓</span>
              </div>
            </el-form>
            <div v-if="imgResultUrl" class="mt">
              <el-image :src="imgResultUrl" fit="contain" style="max-width:100%;max-height:300px;border-radius:8px;"
                :preview-src-list="[imgResultUrl]" />
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-col>
    </el-row>

    <!-- Collect to library dialog: one-tap save like 不背单词 -->
    <el-dialog v-model="collectVisible" title="收藏到文案库" width="420px" :close-on-click-modal="false">
      <div v-loading="collectLoading" class="collect-group-list">
        <!-- Default group -->
        <button class="collect-group-btn" @click="doCollectTo('默认')">
          <span class="collect-group-icon">📁</span>
          <span class="collect-group-name">默认</span>
          <span class="collect-group-arrow">›</span>
        </button>

        <!-- Existing groups -->
        <button
          v-for="g in collectGroups"
          :key="g"
          class="collect-group-btn"
          @click="doCollectTo(g)"
        >
          <span class="collect-group-icon">📁</span>
          <span class="collect-group-name">{{ g }}</span>
          <span class="collect-group-arrow">›</span>
        </button>

        <!-- New group -->
        <div class="collect-new-group">
          <button
            v-if="!showNewGroupInput"
            class="collect-group-btn new-btn"
            @click="showNewGroupInput = true"
          >
            <span class="collect-group-icon">➕</span>
            <span class="collect-group-name">新建分组</span>
          </button>
          <div v-else class="new-group-input-row">
            <el-input
              ref="newGroupInputRef"
              v-model="collectNewGroup"
              placeholder="输入新分组名"
              maxlength="20"
              size="small"
              @keyup.enter="doCollectTo(collectNewGroup)"
            />
            <el-button size="small" type="primary" @click="doCollectTo(collectNewGroup)" :disabled="!collectNewGroup.trim()">确定</el-button>
            <el-button size="small" @click="showNewGroupInput = false; collectNewGroup = ''">取消</el-button>
          </div>
        </div>
      </div>

      <p class="collect-hint">点击分组即可立即收藏，无需二次确认</p>

      <template #footer>
        <el-button @click="collectVisible = false" :disabled="collectLoading">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import draggable from 'vuedraggable'
import http, { type ApiResponse } from '../api/http'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()

interface Product {
  id: number
  name: string
  price: number
  stock: number
  categoryId?: number
  status: string
  shortTitle?: string
  detailContent?: string
}

interface ImageItem {
  id: number
  fileName: string
  url: string
  isMain: number
  selected: boolean
}

interface CopyItem {
  id: number
  platform: string
  variantNo: number
  title: string
  content: string
  isFavorite: number
}

interface Category { id: number; name: string }

const route = useRoute()
const productId = computed(() => Number(route.params.id))
const product = ref<Product | null>(null)
const categories = ref<Category[]>([])
const images = ref<ImageItem[]>([])
const sellingPoints = ref<string[]>([''])
const keywords = ref('')
const platform = ref('XIAOHONGSHU')
const scenario = ref('')
const copies = ref<CopyItem[]>([])
const complianceWarnings = ref<string[]>([])
const pageLoading = ref(false)
const descLoading = ref(false)
const copyLoading = ref(false)
const imgLoading = ref(false)
const descError = ref(false)
const imgError = ref(false)
const imgResultUrl = ref('')
const imgDraftSaving = ref(false)
const imgDraftSaved = ref(false)
const imgFillLoading = ref(false)

// Key prefix for localStorage image-gen drafts
const IMG_DRAFT_PREFIX = 'imgGen_draft_'

// Collect to library dialog state
const collectVisible = ref(false)
const collectLoading = ref(false)
const collectCopyId = ref(0)
const collectGroups = ref<string[]>([])
const collectNewGroup = ref('')
const showNewGroupInput = ref(false)
const newGroupInputRef = ref()

const imgGen = ref({
  sectionTitle: '',
  sectionCopy: '',
  visualDirection: '',
  aspectRatio: '3:4',
})

// Pipeline state
const pipelineSteps = ref<string[]>(['description', 'copy', 'image'])
const pipelinePlatform = ref('XIAOHONGSHU')
const pipelineRunning = ref(false)
const pipelineStepIndex = ref(0)
const pipelineMsg = ref('')
const pipelineResults = ref<Record<string, any>>({})
const activePanels = ref<string[]>([])

const previewList = computed(() => images.value.map((i) => i.url))
const selectedImageIds = computed(() => images.value.filter((i) => i.selected).map((i) => i.id))

function platformLabel(p: string) {
  return { XIAOHONGSHU: '小红书', TAOBAO: '淘宝', DOUYIN: '抖音', OTHER: '其它' }[p] || p
}

async function load() {
  pageLoading.value = true
  try {
    const [detailRes, catRes, copyRes] = await Promise.all([
      http.get<ApiResponse<any>>(`/products/${productId.value}`),
      http.get<ApiResponse<Category[]>>('/categories'),
      http.get<ApiResponse<CopyItem[]>>(`/products/${productId.value}/marketing-copies`),
    ])
    product.value = detailRes.data.data.product
    categories.value = catRes.data.data
    sellingPoints.value = detailRes.data.data.sellingPoints?.length
      ? detailRes.data.data.sellingPoints
      : ['']
    images.value = detailRes.data.data.images.map((img: any) => ({
      ...img,
      selected: true,
    }))
    // 只显示当前商品的文案
    copies.value = copyRes.data.data || []
    // 重置结果
    complianceWarnings.value = []
    imgResultUrl.value = ''
    pipelineResults.value = {}
    // 加载已暂存的详情图表单草稿
    loadImgGenDraft()
    imgDraftSaved.value = false
  } finally {
    pageLoading.value = false
  }
}

// 切换商品时重新加载
watch(productId, () => {
  if (productId.value) load()
})

onMounted(load)

async function saveProduct() {
  if (!product.value) return
  await http.put(`/products/${productId.value}`, {
    ...product.value,
    sellingPoints: sellingPoints.value.filter((s) => s.trim()),
  })
  ElMessage.success('已保存')
}

// 防止重复上传的标记
let uploading = false
async function onFileChange(_uploadFile: UploadFile, uploadFiles: UploadFile[]) {
  if (uploading) return
  const ready = uploadFiles.filter((f) => f.raw && f.status === 'ready')
  if (ready.length === 0) return
  // 只上传本次新增的文件（排除已标记为 success 的）
  const fresh = ready.filter((f) => !(f as any).__uploaded)
  if (fresh.length === 0) return
  uploading = true
  const form = new FormData()
  fresh.forEach((f) => { f.raw && form.append('files', f.raw); (f as any).__uploaded = true })
  try {
    await http.post(`/products/${productId.value}/images/batch`, form)
    ElMessage.success(`已上传 ${fresh.length} 张图片`)
    fresh.forEach((f) => { f.status = 'success' })
    await load()
  } catch (err: any) {
    fresh.forEach((f) => { f.status = 'fail'; (f as any).__uploaded = false })
    const msg = err?.response?.data?.error?.message || err?.message || '上传失败'
    ElMessage.error(msg)
  } finally {
    uploading = false
  }
}

async function removeImage(id: number) {
  await http.delete(`/products/images/${id}`)
  await load()
}

async function downloadImage(id: number) {
  const res = await http.get(`/products/images/${id}/export`, { responseType: 'blob' })
  const blob = new Blob([res.data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `image-${id}.jpg`
  a.click()
  URL.revokeObjectURL(url)
}

async function exportImages(command: string) {
  if (command === 'all') {
    const ids = images.value.map((i) => i.id)
    const res = await http.post(`/products/${productId.value}/images/export-batch`,
      { ids },
      { responseType: 'blob' }
    )
    const blob = new Blob([res.data])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `product-${productId.value}-images.zip`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('全部图片已导出')
  } else if (command === 'selected') {
    const selectedIds = images.value.filter((i) => i.selected).map((i) => i.id)
    if (selectedIds.length === 0) {
      ElMessage.warning('请先勾选要导出的图片')
      return
    }
    const res = await http.post(`/products/${productId.value}/images/export-batch`,
      { ids: selectedIds },
      { responseType: 'blob' }
    )
    const blob = new Blob([res.data])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `product-${productId.value}-images.zip`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('选中图片已导出')
  }
}

async function onReorder() {
  const ids = images.value.map((i) => i.id)
  await http.put(`/products/${productId.value}/images/reorder`, { ids })
  await load()
}

function syncSelected() {
  /* v-model handles */
}

async function generateDetailImage() {
  imgLoading.value = true
  imgError.value = false
  imgResultUrl.value = ''
  let ok = false
  try {
    const { data } = await http.post<ApiResponse<any>>(`/ai/products/${productId.value}/detail-image`, {
      sectionTitle: imgGen.value.sectionTitle,
      sectionCopy: imgGen.value.sectionCopy,
      visualDirection: imgGen.value.visualDirection,
      aspectRatio: imgGen.value.aspectRatio,
    })
    imgResultUrl.value = data.data.url
    clearImgGenDraft()
    imgDraftSaved.value = false
    ElMessage.success('详情图已生成并存入图片库')
    userStore.refreshQuota()
    ok = true
  } catch {
    imgError.value = true
    ElMessage.error('详情图生成失败，请稍后重试')
  } finally {
    imgLoading.value = false
  }
  // 生成完成后再刷新页面数据（后台执行，不阻塞 loading 状态）
  if (ok) {
    load().catch(() => {})
  }
}

async function autoFillDetailImage() {
  if (images.value.length === 0) {
    ElMessage.warning('请先上传商品图片')
    return
  }
  if (!product.value?.name || !product.value.name.trim()) {
    ElMessage.warning('请先在左侧「基础信息」中填写商品名称（如"洗发水""手工皂""台灯"），否则 AI 可能识别错误')
    return
  }
  imgFillLoading.value = true
  try {
    const { data } = await http.post<ApiResponse<any>>(
      `/ai/products/${productId.value}/detail-image-suggestions`,
      { imageIds: selectedImageIds.value.length > 0 ? selectedImageIds.value : undefined }
    )
    if (data.data) {
      if (data.data.sectionTitle) imgGen.value.sectionTitle = data.data.sectionTitle
      if (data.data.sectionCopy) imgGen.value.sectionCopy = data.data.sectionCopy
      if (data.data.visualDirection) imgGen.value.visualDirection = data.data.visualDirection
      ElMessage.success('AI 已根据商品图片填写表单，请检查并修改后生成')
    }
  } catch (err: any) {
    const msg = err?.response?.data?.error?.message || err?.message || '分析失败'
    ElMessage.error('一键填写失败：' + msg)
  } finally {
    imgFillLoading.value = false
  }
}

// ---- 详情图表单草稿 ----
function saveImgGenDraft() {
  imgDraftSaving.value = true
  try {
    const key = IMG_DRAFT_PREFIX + productId.value
    localStorage.setItem(key, JSON.stringify(imgGen.value))
    imgDraftSaved.value = true
    setTimeout(() => { imgDraftSaved.value = false }, 2000)
  } finally {
    imgDraftSaving.value = false
  }
}

function loadImgGenDraft() {
  try {
    const key = IMG_DRAFT_PREFIX + productId.value
    const raw = localStorage.getItem(key)
    if (raw) {
      const saved = JSON.parse(raw)
      if (saved.sectionTitle) imgGen.value.sectionTitle = saved.sectionTitle
      if (saved.sectionCopy) imgGen.value.sectionCopy = saved.sectionCopy
      if (saved.visualDirection) imgGen.value.visualDirection = saved.visualDirection
      if (saved.aspectRatio) imgGen.value.aspectRatio = saved.aspectRatio
    }
  } catch { /* ignore corrupt data */ }
}

function clearImgGenDraft() {
  const key = IMG_DRAFT_PREFIX + productId.value
  localStorage.removeItem(key)
}

// ==================== 一键流水线 ====================
async function runPipeline() {
  if (pipelineSteps.value.length === 0) {
    ElMessage.warning('请至少选择一个步骤')
    return
  }
  pipelineRunning.value = true
  pipelineResults.value = {}
  pipelineStepIndex.value = -1
  pipelineMsg.value = '正在准备...'

  const stepLabels: Record<string, string> = { description: '商品描述', copy: '营销文案', image: '详情图' }
  const stepOrder = ['description', 'copy', 'image']
  const token = localStorage.getItem('token')

  try {
    const response = await fetch(`/api/ai/products/${productId.value}/pipeline/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify({
        steps: pipelineSteps.value,
        imageIds: selectedImageIds.value,
        keywords: keywords.value,
        platform: pipelinePlatform.value,
        variantCount: 3,
        imageSectionTitle: imgGen.value.sectionTitle || undefined,
        imageSectionCopy: imgGen.value.sectionCopy || undefined,
        imageVisualDirection: imgGen.value.visualDirection || undefined,
        imageAspectRatio: imgGen.value.aspectRatio || '3:4',
      }),
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // 解析 SSE 事件
      const lines = buffer.split('\n')
      buffer = lines.pop() || '' // 保留不完整的行
      let eventName = ''
      let eventData = ''

      for (const line of lines) {
        if (line.startsWith('event: ')) {
          eventName = line.slice(7).trim()
        } else if (line.startsWith('data: ')) {
          eventData = line.slice(6).trim()
        } else if (line === '' && eventName && eventData) {
          handleSSEEvent(eventName, eventData, stepLabels, stepOrder)
          eventName = ''
          eventData = ''
        }
      }
    }
  } catch (err: any) {
    ElMessage.error(`流水线执行失败：${err.message || '网络错误'}`)
  } finally {
    pipelineRunning.value = false
    pipelineStepIndex.value = pipelineSteps.value.length
    pipelineMsg.value = '全部完成！'
    userStore.refreshQuota()
  }
}

function handleSSEEvent(
  eventName: string,
  eventData: string,
  stepLabels: Record<string, string>,
  stepOrder: string[]
) {
  if (eventName === 'progress') {
    try {
      const evt = JSON.parse(eventData)
      const step = evt.step as string
      const stepIdx = stepOrder.indexOf(step)

      if (evt.status === 'running') {
        pipelineStepIndex.value = stepIdx
        pipelineMsg.value = `正在生成 ${stepLabels[step]}...`
      } else if (evt.status === 'done') {
        pipelineResults.value[step] = evt.result || true

        if (step === 'description' && evt.result) {
          product.value!.shortTitle = evt.result.shortTitle || product.value!.shortTitle
          product.value!.detailContent = evt.result.detailContent || product.value!.detailContent
          sellingPoints.value = evt.result.sellingPoints || sellingPoints.value
          complianceWarnings.value = evt.result.complianceWarnings || []
        }
        if (step === 'copy') {
          // 异步刷新文案列表
          http.get<ApiResponse<CopyItem[]>>(`/products/${productId.value}/marketing-copies`).then(res => {
            copies.value = (res.data.data || []).filter((c: CopyItem) => c.platform === pipelinePlatform.value)
          }).catch(() => {})
        }
        if (step === 'image') {
          imgResultUrl.value = evt.result?.url || ''
          load().catch(() => {})
        }
      } else if (evt.status === 'failed') {
        pipelineMsg.value = `${stepLabels[step]} 失败：${evt.error || '未知错误'}`
        ElMessage.error(`${stepLabels[step]} 生成失败`)
      }
    } catch { /* JSON 解析失败，忽略 */ }
  } else if (eventName === 'complete') {
    ElMessage.success('组合生成完成')
  } else if (eventName === 'error') {
    ElMessage.error(`流水线错误：${eventData}`)
  }
}

async function generateDescription() {
  if (selectedImageIds.value.length === 0) {
    ElMessage.warning('请至少选择一张图片')
    return
  }
  descLoading.value = true
  descError.value = false
  try {
    const { data } = await http.post<ApiResponse<any>>(`/ai/products/${productId.value}/description`, {
      imageIds: selectedImageIds.value,
      keywords: keywords.value,
    })
    product.value!.shortTitle = data.data.shortTitle
    product.value!.detailContent = data.data.detailContent
    sellingPoints.value = data.data.sellingPoints || ['']
    complianceWarnings.value = data.data.complianceWarnings || []
    ElMessage.success('商品描述已生成，可编辑后保存')
    userStore.refreshQuota()
  } catch {
    descError.value = true
    ElMessage.error('商品描述生成失败，请检查图片和关键词后重试')
  } finally {
    descLoading.value = false
  }
}

async function generateCopy(isRetry: boolean) {
  if (copies.value.length >= 3 && !isRetry) {
    ElMessage.warning('每个商品最多保留3条文案，请先删除旧文案再生成')
    return
  }
  copyLoading.value = true
  let ok = false
  try {
    await http.post(`/ai/products/${productId.value}/marketing-copy`, {
      platform: platform.value,
      variantCount: isRetry ? 3 : 1,
      scenario: scenario.value || undefined,
    })
    ElMessage.success(isRetry ? '已重新生成' : '营销文案已生成')
    userStore.refreshQuota()
    ok = true
  } finally {
    copyLoading.value = false
  }
  // 生成完成后再刷新文案列表（后台执行）
  if (ok) {
    try {
      const { data } = await http.get<ApiResponse<CopyItem[]>>(`/products/${productId.value}/marketing-copies`)
      copies.value = (data.data || []).filter((c) => c.platform === platform.value)
    } catch { /* ignore */ }
  }
}

async function saveCopy(copy: CopyItem) {
  await http.put(`/marketing-copies/${copy.id}`, { title: copy.title, content: copy.content })
  ElMessage.success('文案已保存')
}

async function toggleFavorite(copy: CopyItem) {
  const { data } = await http.post<ApiResponse<CopyItem>>(`/marketing-copies/${copy.id}/favorite`)
  copy.isFavorite = data.data.isFavorite
}

async function deleteCopyItem(id: number) {
  await ElMessageBox.confirm('确定删除该文案？', '提示', { type: 'warning' })
  await http.delete(`/marketing-copies/${id}`)
  ElMessage.success('文案已删除')
  copies.value = copies.value.filter((c) => c.id !== id)
}

async function collectToLibrary(copyId: number) {
  collectCopyId.value = copyId
  collectNewGroup.value = ''
  showNewGroupInput.value = false
  collectVisible.value = true
  // 异步加载分组列表
  try {
    const { data } = await http.get<ApiResponse<string[]>>('/library/groups')
    collectGroups.value = (data.data || []).filter(g => g !== '默认')
  } catch (err: any) {
    console.error('加载分组失败:', err)
    collectGroups.value = []
  }
}

async function doCollectTo(groupName: string) {
  const name = (groupName || '').trim()
  if (!name) {
    ElMessage.warning('请输入分组名称')
    return
  }
  collectLoading.value = true
  try {
    await http.post('/library', { copyId: collectCopyId.value, groupName: name })
    ElMessage.success(`已收藏到「${name}」`)
    collectVisible.value = false
  } catch (err: any) {
    const msg = err?.response?.data?.error?.message || err?.message || '请稍后重试'
    ElMessage.error('收藏失败：' + msg)
  } finally {
    collectLoading.value = false
  }
}

// 展开新建分组输入框时自动聚焦
watch(showNewGroupInput, async (val) => {
  if (val) {
    await nextTick()
    newGroupInputRef.value?.focus?.()
  }
})

async function exportCopy(id: number, format: string) {
  const res = await http.get(`/marketing-copies/${id}/export`, {
    params: { format },
    responseType: 'blob',
  })
  const blob = new Blob([res.data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const ext = format === 'md' ? 'md' : format === 'pdf' ? 'pdf' : 'txt'
  a.download = `copy-${id}.${ext}`
  a.click()
  URL.revokeObjectURL(url)
}

async function exportBatch(format: string) {
  const ids = copies.value.map((c) => c.id)
  if (ids.length === 0) return
  const res = await http.post('/marketing-copies/export-batch',
    { ids, format },
    { responseType: 'blob' }
  )
  const blob = new Blob([res.data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = format === 'xlsx' ? 'copies-batch.xlsx' : 'copies-batch.txt'
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('批量导出完成')
}
</script>

<style scoped>
.page { padding: 8px; max-width: 1400px; }

/* Override page header */
.page :deep(.el-page-header__content) {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-weight: 600;
}
.page :deep(.el-page-header__back) {
  color: var(--text-muted);
}

.mt { margin-top: 16px; }
.mb { margin-bottom: 8px; }
.ml { margin-left: 8px; }

.selling-points { width: 100%; }
.sp-row { display: flex; margin-bottom: 8px; gap: 8px; }
.image-grid { display: flex; flex-wrap: wrap; margin: 10px -6px -6px -6px; }
.img-card {
  width: 120px; text-align: center; margin: 6px;
  background: rgba(0,0,0,0.02);
  border-radius: var(--radius-md);
  padding: 8px;
  border: 1px solid var(--border-subtle);
}
.thumb { width: 120px; height: 120px; border-radius: 6px; }

.copy-item {
  margin-top: 16px; padding: 16px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: rgba(0,0,0,0.02);
}
.copy-head { display: flex; align-items: center; flex-wrap: wrap; margin: -4px; margin-bottom: 8px; }
.copy-head > * { margin: 4px; }

/* Pipeline card — the core feature */
.pipeline-card {
  border-radius: var(--radius-lg) !important;
  border: 1px solid rgba(196,155,74,0.25) !important;
  background: linear-gradient(135deg, rgba(196,155,74,0.04), rgba(94,138,138,0.03)) !important;
  box-shadow: 0 0 30px rgba(196,155,74,0.05);
}
.pipeline-steps { margin-bottom: 12px; }
.step-row {
  display: flex; align-items: center; gap: 8px; padding: 10px 14px;
  border-radius: var(--radius-md); margin-bottom: 4px;
  transition: all 0.25s var(--ease-out);
  border: 1px solid transparent;
}
.step-row.active {
  background: rgba(196,155,74,0.06);
  border-color: rgba(196,155,74,0.15);
}
.step-label { font-weight: 600; font-size: 14px; min-width: 90px; color: var(--text-primary); }
.step-hint { font-size: 12px; color: var(--text-muted); flex: 1; }
.pipeline-params { margin-bottom: 12px; padding: 8px 0; display: flex; align-items: center; color: var(--text-secondary); }
.pipeline-btn { width: 100%; height: 44px; font-size: 16px; margin-top: 4px; border-radius: var(--radius-md); }
.pipeline-progress { margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--border-subtle); }
.pipeline-progress :deep(.el-step__title) { color: var(--text-secondary) !important; font-size: 12px; }
.pipeline-progress :deep(.el-step__title.is-process) { color: var(--accent) !important; }
.pipeline-progress :deep(.el-step__title.is-finish) { color: var(--success) !important; }
.progress-msg { text-align: center; color: var(--accent); font-size: 13px; margin-top: 8px; }
.result-preview {
  padding: 12px 16px; background: rgba(0,0,0,0.03);
  border-radius: var(--radius-md); font-size: 13px;
  border: 1px solid var(--border-subtle);
}
.result-preview p { margin: 4px 0; color: var(--text-secondary); }
.result-preview strong { color: var(--text-primary); }

/* Collapse */
.page :deep(.el-collapse-item__header) {
  color: var(--text-primary);
  font-weight: 600;
  border-bottom: 1px solid var(--border-subtle);
}
.page :deep(.el-collapse-item__wrap) {
  background: transparent;
  border-bottom: 1px solid var(--border-subtle);
}
.page :deep(.el-collapse-item__content) {
  color: var(--text-secondary);
}

/* Alert */
.page :deep(.el-alert--warning) {
  background: rgba(212,164,74,0.08);
  border: 1px solid rgba(212,164,74,0.2);
}

/* Collect dialog — "不背单词" style group list */
.collect-group-list {
  display: flex; flex-direction: column; gap: 8px;
}
.collect-group-btn {
  display: flex; align-items: center; gap: 10px;
  width: 100%; padding: 14px 16px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--bg-surface);
  cursor: pointer;
  font-size: 15px; font-family: inherit;
  transition: all 0.2s var(--ease-out);
  text-align: left; color: var(--text-primary);
}
.collect-group-btn:hover {
  border-color: rgba(196,155,74,0.4);
  background: rgba(196,155,74,0.04);
  transform: translateX(4px);
}
.collect-group-btn:active { transform: scale(0.98); }
.collect-group-icon { font-size: 20px; flex-shrink: 0; }
.collect-group-name { flex: 1; font-weight: 500; }
.collect-group-arrow {
  font-size: 22px; color: var(--text-muted);
  font-weight: 300; flex-shrink: 0;
}
.collect-group-btn.new-btn {
  border-style: dashed; color: var(--accent);
  justify-content: flex-start;
}
.collect-group-btn.new-btn:hover {
  border-color: var(--accent);
  background: rgba(196,155,74,0.06);
}
.collect-group-btn.new-btn .collect-group-arrow { display: none; }
.new-group-input-row {
  display: flex; gap: 8px; align-items: center;
  padding: 8px 12px;
  border: 1px dashed var(--accent);
  border-radius: var(--radius-md);
  background: rgba(196,155,74,0.03);
}
.collect-hint {
  margin: 16px 0 0; font-size: 12px;
  color: var(--text-muted); text-align: center;
}
</style>

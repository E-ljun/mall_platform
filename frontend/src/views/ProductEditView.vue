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
            <el-radio-group v-model="platform" size="small">
              <el-radio-button label="XIAOHONGSHU">小红书</el-radio-button>
              <el-radio-button label="TAOBAO">淘宝</el-radio-button>
              <el-radio-button label="DOUYIN">抖音</el-radio-button>
              <el-radio-button label="OTHER">其它</el-radio-button>
            </el-radio-group>
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
            <el-form label-width="80px" size="small">
              <el-form-item label="模块标题">
                <el-input v-model="imgGen.sectionTitle" placeholder="如：核心参数对比" />
              </el-form-item>
              <el-form-item label="文案要点">
                <el-input v-model="imgGen.sectionCopy" type="textarea" :rows="2" placeholder="图中需包含的信息" />
              </el-form-item>
              <el-form-item label="画面描述">
                <el-input v-model="imgGen.visualDirection" placeholder="如：科技感蓝黑背景，左侧参数表" />
              </el-form-item>
              <el-form-item label="比例">
                <el-select v-model="imgGen.aspectRatio" style="width:100px">
                  <el-option label="3:4" value="3:4" />
                  <el-option label="1:1" value="1:1" />
                  <el-option label="16:9" value="16:9" />
                  <el-option label="9:16" value="9:16" />
                </el-select>
              </el-form-item>
              <el-button type="primary" :loading="imgLoading" @click="generateDetailImage">生成</el-button>
              <el-button v-if="imgError" @click="generateDetailImage">重试</el-button>
            </el-form>
            <div v-if="imgResultUrl" class="mt">
              <el-image :src="imgResultUrl" fit="contain" style="max-width:100%;max-height:300px;border-radius:8px;"
                :preview-src-list="[imgResultUrl]" />
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
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
const copies = ref<CopyItem[]>([])
const complianceWarnings = ref<string[]>([])
const pageLoading = ref(false)
const descLoading = ref(false)
const copyLoading = ref(false)
const imgLoading = ref(false)
const descError = ref(false)
const imgError = ref(false)
const imgResultUrl = ref('')

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
  try {
    const { data } = await http.post<ApiResponse<any>>(`/ai/products/${productId.value}/detail-image`, {
      sectionTitle: imgGen.value.sectionTitle,
      sectionCopy: imgGen.value.sectionCopy,
      visualDirection: imgGen.value.visualDirection,
      aspectRatio: imgGen.value.aspectRatio,
    })
    imgResultUrl.value = data.data.url
    ElMessage.success('详情图已生成并存入图片库')
    userStore.refreshQuota()
    await load()
  } catch {
    imgError.value = true
  } finally {
    imgLoading.value = false
  }
}

// ==================== 一键流水线 ====================
async function runPipeline() {
  if (pipelineSteps.value.length === 0) {
    ElMessage.warning('请至少选择一个步骤')
    return
  }
  pipelineRunning.value = true
  pipelineResults.value = {}
  pipelineStepIndex.value = 0
  pipelineMsg.value = '正在准备...'

  const stepList = pipelineSteps.value
  const stepLabels: Record<string, string> = { description: '商品描述', copy: '营销文案', image: '详情图' }

  for (let i = 0; i < stepList.length; i++) {
    const step = stepList[i]
    pipelineStepIndex.value = i
    pipelineMsg.value = `正在生成 ${stepLabels[step]}...`

    try {
      const { data } = await http.post<ApiResponse<any>>(`/ai/products/${productId.value}/pipeline`, {
        steps: [step],
        imageIds: selectedImageIds.value,
        keywords: keywords.value,
        platform: pipelinePlatform.value,
        variantCount: 3,
        imageSectionTitle: imgGen.value.sectionTitle || undefined,
        imageSectionCopy: imgGen.value.sectionCopy || undefined,
        imageVisualDirection: imgGen.value.visualDirection || undefined,
        imageAspectRatio: imgGen.value.aspectRatio || '3:4',
      })

      if (data.success) {
        const result = data.data
        pipelineResults.value[step] = result[step] || result

        if (step === 'description' && result.description) {
          product.value!.shortTitle = result.description.shortTitle || product.value!.shortTitle
          product.value!.detailContent = result.description.detailContent || product.value!.detailContent
          sellingPoints.value = result.description.sellingPoints || sellingPoints.value
          complianceWarnings.value = result.description.complianceWarnings || []
        }
        if (step === 'copy') {
          const copyRes = await http.get<ApiResponse<CopyItem[]>>(`/products/${productId.value}/marketing-copies`)
          copies.value = (copyRes.data.data || []).filter((c) => c.platform === pipelinePlatform.value)
        }
        if (step === 'image') {
          await load()
          imgResultUrl.value = result.image?.url || result.url || ''
        }
      }
    } catch (err: any) {
      pipelineMsg.value = `${stepLabels[step]} 失败：${err.response?.data?.error?.message || err.message || '未知错误'}`
      ElMessage.error(`${stepLabels[step]} 失败`)
    }
  }

  pipelineStepIndex.value = stepList.length
  pipelineMsg.value = '全部完成！'
  pipelineRunning.value = false
  userStore.refreshQuota()
  ElMessage.success('组合生成完成')
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
  try {
    await http.post(`/ai/products/${productId.value}/marketing-copy`, {
      platform: platform.value,
      variantCount: isRetry ? 3 : 1,
    })
    ElMessage.success(isRetry ? '已重新生成' : '营销文案已生成')
    userStore.refreshQuota()
    const { data } = await http.get<ApiResponse<CopyItem[]>>(`/products/${productId.value}/marketing-copies`)
    // 仅显示当前产品、当前平台的文案
    copies.value = (data.data || []).filter((c) => c.platform === platform.value)
  } finally {
    copyLoading.value = false
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
.page { padding: 8px; }
.mt { margin-top: 16px; }
.mb { margin-bottom: 8px; }
.ml { margin-left: 8px; }
.selling-points { width: 100%; }
.sp-row { display: flex; margin-bottom: 8px; }
.sp-row > * + * { margin-left: 8px; }
.image-grid { display: flex; flex-wrap: wrap; margin: 10px -6px -6px -6px; }
.img-card { width: 120px; text-align: center; margin: 6px; }
.thumb { width: 120px; height: 120px; border-radius: 6px; }
.copy-item { margin-top: 16px; padding-top: 16px; border-top: 1px solid #eee; }
.copy-head { display: flex; align-items: center; flex-wrap: wrap; margin: -4px; margin-bottom: 4px; }
.copy-head > * { margin: 4px; }

.pipeline-card { border-radius: 12px; border: 2px solid #667eea; background: linear-gradient(to bottom, #fafbff, #fff); }
.pipeline-steps { margin-bottom: 12px; }
.step-row {
  display: flex; align-items: center; gap: 8px; padding: 8px 12px;
  border-radius: 8px; margin-bottom: 4px; transition: background 0.2s;
}
.step-row.active { background: #f0f2ff; }
.step-label { font-weight: 600; font-size: 14px; min-width: 90px; }
.step-hint { font-size: 12px; color: #999; flex: 1; }
.pipeline-params { margin-bottom: 12px; padding: 8px 0; display: flex; align-items: center; }
.pipeline-btn { width: 100%; height: 44px; font-size: 16px; margin-top: 4px; }
.pipeline-progress { margin-top: 16px; padding-top: 16px; border-top: 1px solid #eee; }
.progress-msg { text-align: center; color: #667eea; font-size: 13px; margin-top: 8px; }
.result-preview { padding: 8px 12px; background: #f5f7fa; border-radius: 8px; font-size: 13px; }
.result-preview p { margin: 4px 0; }
</style>

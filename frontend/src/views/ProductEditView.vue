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
                <el-option label="草稿" value="DRAFT" />
                <el-option label="上架" value="ON_SHELF" />
                <el-option label="下架" value="OFF_SHELF" />
              </el-select>
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
          <template #header>商品图片（拖拽排序，首张为主图，最多10张）</template>
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
              </div>
            </template>
          </draggable>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>AI 商品描述生成</template>
          <el-alert v-if="complianceWarnings.length" type="warning" :closable="false" class="mb">
            <div v-for="(w, i) in complianceWarnings" :key="i">{{ w }}</div>
          </el-alert>
          <el-button type="success" :loading="descLoading" @click="generateDescription">生成商品描述</el-button>
          <el-button v-if="descError" @click="generateDescription">重试</el-button>
        </el-card>

        <el-card class="mt">
          <template #header>多平台营销文案</template>
          <el-radio-group v-model="platform">
            <el-radio-button label="XIAOHONGSHU">小红书</el-radio-button>
            <el-radio-button label="TAOBAO">淘宝</el-radio-button>
            <el-radio-button label="DOUYIN">抖音</el-radio-button>
          </el-radio-group>
          <el-button type="primary" class="ml" :loading="copyLoading" @click="generateCopy(false)">生成文案</el-button>
          <el-button :loading="copyLoading" @click="generateCopy(true)">换一换</el-button>

          <div v-for="copy in copies" :key="copy.id" class="copy-item">
            <div class="copy-head">
              <el-tag size="small">{{ platformLabel(copy.platform) }}</el-tag>
              <el-tag size="small" type="info">版本 {{ copy.variantNo }}</el-tag>
              <el-button link :type="copy.isFavorite ? 'warning' : 'default'" @click="toggleFavorite(copy)">
                {{ copy.isFavorite ? '已收藏' : '收藏' }}
              </el-button>
              <el-dropdown @command="(fmt: string) => exportCopy(copy.id, fmt)">
                <el-button link type="primary">导出</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="txt">TXT</el-dropdown-item>
                    <el-dropdown-item command="md">Markdown</el-dropdown-item>
                    <el-dropdown-item command="pdf">PDF</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <el-input v-model="copy.title" placeholder="标题" class="mb" />
            <el-input v-model="copy.content" type="textarea" :rows="5" />
            <el-button size="small" class="mt" @click="saveCopy(copy)">保存文案</el-button>
          </div>
          <div v-if="copies.length > 0" style="margin-top: 16px; padding-top: 16px; border-top: 1px solid #eee;">
            <el-dropdown @command="exportBatch">
              <el-button type="primary" plain>批量导出 ({{ copies.length }} 条)</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="txt">导出 TXT</el-dropdown-item>
                  <el-dropdown-item command="xlsx">导出 Excel</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <el-empty v-if="copies.length === 0" description="暂无文案，点击生成" />
        </el-card>

        <el-card class="mt">
          <template #header>AI 详情图生成 (Wan 2.7)</template>
          <el-form label-width="100px">
            <el-form-item label="模块标题">
              <el-input v-model="imgGen.sectionTitle" placeholder="如：核心参数对比" />
            </el-form-item>
            <el-form-item label="文案要点">
              <el-input v-model="imgGen.sectionCopy" type="textarea" :rows="2" placeholder="图中需包含的关键信息" />
            </el-form-item>
            <el-form-item label="画面描述">
              <el-input v-model="imgGen.visualDirection" placeholder="如：科技感蓝黑渐变背景，左侧放参数表" />
            </el-form-item>
            <el-form-item label="比例">
              <el-select v-model="imgGen.aspectRatio" style="width: 120px">
                <el-option label="3:4" value="3:4" />
                <el-option label="1:1" value="1:1" />
                <el-option label="16:9" value="16:9" />
                <el-option label="9:16" value="9:16" />
              </el-select>
            </el-form-item>
            <el-button type="primary" :loading="imgLoading" @click="generateDetailImage">
              生成详情图
            </el-button>
            <el-button v-if="imgError" @click="generateDetailImage">重试</el-button>
          </el-form>
          <div v-if="imgResultUrl" style="margin-top: 12px;">
            <el-image :src="imgResultUrl" fit="contain" style="max-width: 100%; max-height: 400px; border-radius: 8px;"
              :preview-src-list="[imgResultUrl]" />
            <div style="margin-top: 8px; color: #666; font-size: 13px;">
              已自动存入商品图片库，可在左侧"商品图片"中查看
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import draggable from 'vuedraggable'
import http, { type ApiResponse } from '../api/http'

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
const productId = Number(route.params.id)
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

const previewList = computed(() => images.value.map((i) => i.url))
const selectedImageIds = computed(() => images.value.filter((i) => i.selected).map((i) => i.id))

function platformLabel(p: string) {
  return { XIAOHONGSHU: '小红书', TAOBAO: '淘宝', DOUYIN: '抖音' }[p] || p
}

async function load() {
  pageLoading.value = true
  try {
    const [detailRes, catRes, copyRes] = await Promise.all([
      http.get<ApiResponse<any>>(`/products/${productId}`),
      http.get<ApiResponse<Category[]>>('/categories'),
      http.get<ApiResponse<CopyItem[]>>(`/products/${productId}/marketing-copies`),
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
    copies.value = copyRes.data.data
  } finally {
    pageLoading.value = false
  }
}

async function saveProduct() {
  if (!product.value) return
  await http.put(`/products/${productId}`, {
    ...product.value,
    sellingPoints: sellingPoints.value.filter((s) => s.trim()),
  })
  ElMessage.success('已保存')
}

async function onFileChange(_uploadFile: UploadFile, uploadFiles: UploadFile[]) {
  const ready = uploadFiles.filter((f) => f.raw && f.status === 'ready')
  if (ready.length === 0 || ready.length !== uploadFiles.length) return
  const form = new FormData()
  ready.forEach((f) => f.raw && form.append('files', f.raw))
  ready.forEach((f) => { f.status = 'success' })
  try {
    await http.post(`/products/${productId}/images/batch`, form)
    ElMessage.success(`已上传 ${ready.length} 张图片`)
    await load()
  } catch {
    ready.forEach((f) => { f.status = 'fail' })
  }
}

async function removeImage(id: number) {
  await http.delete(`/products/images/${id}`)
  await load()
}

async function onReorder() {
  const ids = images.value.map((i) => i.id)
  await http.put(`/products/${productId}/images/reorder`, { ids })
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
    const { data } = await http.post<ApiResponse<any>>(`/ai/products/${productId}/detail-image`, {
      sectionTitle: imgGen.value.sectionTitle,
      sectionCopy: imgGen.value.sectionCopy,
      visualDirection: imgGen.value.visualDirection,
      aspectRatio: imgGen.value.aspectRatio,
    })
    imgResultUrl.value = data.data.url
    ElMessage.success('详情图已生成并存入图片库')
    await load() // 刷新图片列表
  } catch {
    imgError.value = true
  } finally {
    imgLoading.value = false
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
    const { data } = await http.post<ApiResponse<any>>(`/ai/products/${productId}/description`, {
      imageIds: selectedImageIds.value,
      keywords: keywords.value,
    })
    product.value!.shortTitle = data.data.shortTitle
    product.value!.detailContent = data.data.detailContent
    sellingPoints.value = data.data.sellingPoints || ['']
    complianceWarnings.value = data.data.complianceWarnings || []
    ElMessage.success('商品描述已生成，可编辑后保存')
  } catch {
    descError.value = true
  } finally {
    descLoading.value = false
  }
}

async function generateCopy(isRetry: boolean) {
  copyLoading.value = true
  try {
    await http.post(`/ai/products/${productId}/marketing-copy`, {
      platform: platform.value,
      variantCount: 3,
    })
    ElMessage.success(isRetry ? '已重新生成' : '营销文案已生成')
    const { data } = await http.get<ApiResponse<CopyItem[]>>(`/products/${productId}/marketing-copies`)
    copies.value = data.data.filter((c) => c.platform === platform.value)
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

onMounted(load)
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
</style>

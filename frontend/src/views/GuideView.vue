<template>
  <div class="guide-page">
    <!-- Hero -->
    <div class="guide-hero">
      <div class="hero-glow"></div>
      <div class="hero-inner">
        <h2>🎨 AI 详情图生成指南</h2>
        <p>Wan 2.7 图像生成 · 从灵感到出图，一站式参考手册</p>
      </div>
    </div>

    <!-- 字段说明 -->
    <section class="guide-section">
      <h3 class="section-heading"><span class="section-icon">📋</span> 三个字段分别填什么？</h3>
      <div class="field-cards">
        <div class="field-card" v-for="f in fieldIntro" :key="f.name">
          <div class="field-icon">{{ f.icon }}</div>
          <h4>{{ f.name }}</h4>
          <p class="field-desc">{{ f.desc }}</p>
          <p class="field-example">{{ f.example }}</p>
        </div>
      </div>
    </section>

    <!-- 风格模板 -->
    <section class="guide-section">
      <h3 class="section-heading"><span class="section-icon">🎭</span> 风格模板 <span class="count-badge">{{ templates.length }} 款</span></h3>
      <p class="section-sub">点击任意模板复制到剪贴板，在商品编辑页粘贴即可</p>
      <div class="tpl-grid">
        <div class="tpl-card" v-for="tpl in templates" :key="tpl.name" @click="copyTemplate(tpl)">
          <div class="tpl-visual" :style="{ background: tpl.bg }">
            <span class="tpl-emoji">{{ tpl.emoji }}</span>
            <span class="tpl-name">{{ tpl.name }}</span>
          </div>
          <div class="tpl-info">
            <div class="tpl-field" @click.stop="copyField($event, tpl.sectionTitle)">
              <label>标题</label>
              <span :title="tpl.sectionTitle">{{ tpl.sectionTitle }}</span>
              <button class="tpl-copy-btn" title="复制此项">📋</button>
            </div>
            <div class="tpl-field" @click.stop="copyField($event, tpl.sectionCopy)">
              <label>文案</label>
              <span :title="tpl.sectionCopy">{{ tpl.sectionCopy }}</span>
              <button class="tpl-copy-btn" title="复制此项">📋</button>
            </div>
            <div class="tpl-field" @click.stop="copyField($event, tpl.visualDirection)">
              <label>画面</label>
              <span :title="tpl.visualDirection">{{ tpl.visualDirection }}</span>
              <button class="tpl-copy-btn" title="复制此项">📋</button>
            </div>
            <div class="tpl-meta">
              <el-tag size="small">{{ tpl.aspectRatio }}</el-tag>
              <span class="tpl-copy-hint">点击行复制 → 点击卡片复制全部</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 背景图库 -->
    <section class="guide-section">
      <h3 class="section-heading"><span class="section-icon">🖼️</span> 背景纹理库 <span class="count-badge">{{ backgrounds.length }} 款</span></h3>
      <p class="section-sub">选中一张背景图，复制对应描述添加到"画面描述"中</p>
      <div class="bg-grid">
        <div
          v-for="(bg, idx) in backgrounds"
          :key="idx"
          :class="['bg-card', { 'bg-selected': selectedBg === idx }]"
          @click="selectBg(idx)"
        >
          <div class="bg-preview" :style="bg.style"></div>
          <div class="bg-label">{{ bg.name }}</div>
          <div class="bg-check" v-if="selectedBg === idx">✓ 已选中</div>
        </div>
      </div>
      <div v-if="selectedBg !== null" class="bg-prompt-box">
        <span class="bg-prompt-label">已选背景描述（复制添加到画面描述）：</span>
        <code>{{ backgrounds[selectedBg].promptAddon }}</code>
        <el-button size="small" type="primary" plain @click="copyBgPrompt">复制背景描述</el-button>
      </div>
    </section>

    <!-- 比例说明 -->
    <section class="guide-section">
      <h3 class="section-heading"><span class="section-icon">📐</span> 四种比例怎么选？</h3>
      <div class="ratio-row">
        <div class="ratio-card" v-for="r in ratios" :key="r.value">
          <div class="ratio-box" :style="{ aspectRatio: r.value.replace(':', '/'), background: r.bg }">
            <span>{{ r.value }}</span>
          </div>
          <p class="ratio-label">{{ r.label }}</p>
          <p class="ratio-use">{{ r.use }}</p>
        </div>
      </div>
    </section>

    <!-- 技巧 -->
    <section class="guide-section">
      <h3 class="section-heading"><span class="section-icon">💡</span> 提升出图质量的技巧</h3>
      <div class="tips-grid">
        <div class="tip-card" v-for="(t, i) in tips" :key="i">
          <span class="tip-num">{{ i + 1 }}</span>
          <div class="tip-body">
            <strong>{{ t.title }}</strong>
            <p>{{ t.detail }}</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const selectedBg = ref<number | null>(null)

// ---- 字段说明 ----
const fieldIntro = [
  { icon: '🏷️', name: '模块标题', desc: '这个详情图叫什么名字', example: '核心参数对比 / 产品亮点展示' },
  { icon: '📝', name: '文案要点', desc: '图片中的文字信息、卖点', example: '8核处理器 | 16GB内存 | 超薄机身' },
  { icon: '🎬', name: '画面描述', desc: '整体风格、配色、构图', example: '深蓝科技感背景，产品悬浮居中' },
]

// ---- 12 种风格模板 ----
const templates = [
  {
    name: '科技参数风', emoji: '🖥️', bg: 'linear-gradient(135deg, #0a1628, #1a2d50)',
    sectionTitle: '核心参数对比',
    sectionCopy: '旗舰处理器 | 120Hz高刷屏 | 5000mAh大电池 | 轻薄机身仅186g',
    visualDirection: '深蓝黑科技感背景，产品3D悬浮居中，左侧参数卡片带发光边框，顶部炫光光效，高端简洁',
    aspectRatio: '3:4',
  },
  {
    name: '简约白底风', emoji: '🤍', bg: 'linear-gradient(135deg, #e8e8e8, #f5f5f5)',
    sectionTitle: '产品展示',
    sectionCopy: '简约设计 | 高品质材质 | 舒适手感 | 多色可选',
    visualDirection: '纯白浅灰渐变背景，柔和自然光照明，极简无多余装饰，苹果官网风格',
    aspectRatio: '1:1',
  },
  {
    name: '温暖生活风', emoji: '🌿', bg: 'linear-gradient(135deg, #f5efe0, #e8dcc8)',
    sectionTitle: '生活场景',
    sectionCopy: '清晨的第一缕阳光 | 温暖陪伴每一天 | 天然材质呵护健康',
    visualDirection: '温暖米色背景，产品置于木质桌面，侧光透过窗帘洒落，绿植点缀，温馨自然',
    aspectRatio: '3:4',
  },
  {
    name: '促销活动风', emoji: '🔥', bg: 'linear-gradient(135deg, #c0392b, #e74c3c)',
    sectionTitle: '限时特惠',
    sectionCopy: '爆款直降100元 | 买一送一 | 限量500件 | 7天无理由退换',
    visualDirection: '红橙渐变活力背景，产品居中，周围放射状光束，顶部大字促销标签，底部倒计时条',
    aspectRatio: '16:9',
  },
  {
    name: '时尚大牌风', emoji: '✨', bg: 'linear-gradient(135deg, #1a1a2e, #2d2d44)',
    sectionTitle: '当季新品',
    sectionCopy: '2026春季限定 | 匠心品质 | 诠释优雅生活',
    visualDirection: '深色高级感背景，产品置于大理石台面，聚光灯效果，金属质感，VOGUE杂志大片风格',
    aspectRatio: '9:16',
  },
  {
    name: '清新自然风', emoji: '🍃', bg: 'linear-gradient(135deg, #d4f5e6, #a8e6cf)',
    sectionTitle: '天然成分',
    sectionCopy: '0添加 | 植物萃取 | 温和不刺激 | 适合敏感肌',
    visualDirection: '浅绿渐变背景，产品置于水中或绿叶环绕，水波纹效果，清新通透，自然疗愈感',
    aspectRatio: '3:4',
  },
  {
    name: '国潮新中式', emoji: '🏮', bg: 'linear-gradient(135deg, #2d1b2e, #4a2030)',
    sectionTitle: '东方雅韵',
    sectionCopy: '传承匠心 | 手工打造 | 东方美学 | 限量发售',
    visualDirection: '深红黑渐变底，金色祥云纹装饰边角，产品居中陈列于红木托盘，暖黄侧光，故宫美学风格',
    aspectRatio: '3:4',
  },
  {
    name: '赛博朋克风', emoji: '🤖', bg: 'linear-gradient(135deg, #0d0221, #150578, #3a0ca3)',
    sectionTitle: '未来科技',
    sectionCopy: '次世代性能 | RGB光效 | AI驱动 | 超频体验',
    visualDirection: '深紫蓝霓虹背景，网格地板，产品悬浮发光，霓虹灯管边框，全息投影数据流，赛博朋克风格',
    aspectRatio: '16:9',
  },
  {
    name: '北欧极简', emoji: '🪑', bg: 'linear-gradient(135deg, #f0ece4, #e8e2d8)',
    sectionTitle: '设计美学',
    sectionCopy: '极简设计 | 自然材质 | 功能至上 | 经久耐用',
    visualDirection: '浅灰白极简背景，产品置于原木色底座，柔和漫射光，大面积留白，MUJI/宜家风格',
    aspectRatio: '1:1',
  },
  {
    name: '母婴温馨', emoji: '🍼', bg: 'linear-gradient(135deg, #fef9f0, #fce4ec)',
    sectionTitle: '安心呵护',
    sectionCopy: '食品级材质 | 安全无味 | 妈妈首选 | 柔护新生',
    visualDirection: '柔和粉色渐变底，产品周围有棉花糖云朵装饰，柔光暖调，旁边摆放婴儿用品，温馨安全感',
    aspectRatio: '3:4',
  },
  {
    name: '商务专业风', emoji: '💼', bg: 'linear-gradient(135deg, #1c2333, #2d3748)',
    sectionTitle: '专业高效',
    sectionCopy: '企业级性能 | 数据安全 | 高效协同 | 7x24h服务',
    visualDirection: '深灰蓝商务背景，产品置于深色皮质台面，侧方金属钢笔，冷调专业光，企业宣传册风格',
    aspectRatio: '16:9',
  },
  {
    name: '美食诱惑风', emoji: '🍜', bg: 'linear-gradient(135deg, #3d1e0a, #5c2d0e)',
    sectionTitle: '舌尖盛宴',
    sectionCopy: '地道风味 | 新鲜食材 | 匠心烹饪 | 一口满足',
    visualDirection: '暖棕深橙色美食背景，产品置于木砧板上，侧逆光勾勒轮廓，热气蒸腾效果，食欲感满满',
    aspectRatio: '1:1',
  },
]

// ---- 21 款背景纹理 ----
const backgrounds = [
  { name: '纯白底', style: 'background: #ffffff;', promptAddon: '纯白色背景，无纹理' },
  { name: '浅灰丝绒', style: 'background: linear-gradient(135deg, #f5f5f5, #e8e8e8);', promptAddon: '浅灰色丝绒质感渐变背景' },
  { name: '暖米纸纹', style: 'background: linear-gradient(45deg, #faf6f0 25%, #f5efe0 75%);', promptAddon: '暖米色纸张纹理感背景' },
  { name: '大理石灰', style: 'background: linear-gradient(135deg, #eaeaea 0%, #d5d5d5 50%, #e0e0e0 100%);', promptAddon: '浅灰色大理石纹理背景' },
  { name: '象牙白', style: 'background: linear-gradient(180deg, #fffef9, #faf8f2);', promptAddon: '象牙白柔和渐变背景' },
  { name: '深空蓝', style: 'background: linear-gradient(180deg, #1a1a3e, #0d1b2a);', promptAddon: '深空蓝色渐变背景，神秘深邃' },
  { name: '墨绿丝绒', style: 'background: linear-gradient(135deg, #1a2f2a, #0f1f1a);', promptAddon: '深墨绿色丝绒质感背景' },
  { name: '酒红帷幕', style: 'background: linear-gradient(180deg, #3d1a1a, #2d0f0f);', promptAddon: '酒红色剧院帷幕感背景' },
  { name: '日暮橙', style: 'background: linear-gradient(135deg, #ff9a56, #ff6b6b);', promptAddon: '日落橙红渐变背景，温暖活力' },
  { name: '冰川蓝', style: 'background: linear-gradient(135deg, #e0f0f8, #c8e0f0);', promptAddon: '冰川蓝色清新渐变背景' },
  { name: '薰衣草紫', style: 'background: linear-gradient(135deg, #f5f0ff, #ede0f5);', promptAddon: '薰衣草淡紫色梦幻渐变背景' },
  { name: '薄荷绿', style: 'background: linear-gradient(180deg, #e8f5e9, #c8e6c9);', promptAddon: '薄荷绿色清新渐变背景' },
  { name: '樱花粉', style: 'background: linear-gradient(135deg, #fce4ec, #f8bbd0);', promptAddon: '樱花粉色柔和渐变背景' },
  { name: '水泥灰', style: 'background: linear-gradient(135deg, #bdbdbd, #9e9e9e);', promptAddon: '水泥灰色工业风纹理背景' },
  { name: '金色绸缎', style: 'background: linear-gradient(135deg, #f5e6c8, #e8d5a3, #d4b87a);', promptAddon: '金色绸缎光泽感渐变背景' },
  { name: '午夜黑', style: 'background: linear-gradient(180deg, #1a1a1a, #0d0d0d);', promptAddon: '纯黑色深邃背景，突出产品主体' },
  { name: '奶油杏', style: 'background: linear-gradient(135deg, #fff5ec, #ffe8d6);', promptAddon: '奶油杏色温暖柔和背景' },
  { name: '雾霾蓝', style: 'background: linear-gradient(135deg, #d5e1eb, #bcccd8);', promptAddon: '雾霾蓝灰高级冷淡风背景' },
  { name: '焦糖棕', style: 'background: linear-gradient(135deg, #d4a574, #c4946c);', promptAddon: '焦糖棕色温暖质感背景' },
  { name: '极光绿', style: 'background: linear-gradient(135deg, #1b4332, #2d6a4f, #40916c);', promptAddon: '极光深绿色渐变背景，自然高级' },
  { name: '星河银', style: 'background: linear-gradient(135deg, #e8e8f0, #d0d0e0, #c0c0d8);', promptAddon: '星河银灰色渐变背景，科技典雅' },
]

function selectBg(idx: number) {
  selectedBg.value = selectedBg.value === idx ? null : idx
}

function copyBgPrompt() {
  if (selectedBg.value === null) return
  navigator.clipboard.writeText(backgrounds[selectedBg.value].promptAddon)
  ElMessage.success('背景描述已复制')
}

function copyField(event: MouseEvent, text: string) {
  event.stopPropagation()
  navigator.clipboard.writeText(text).then(() => ElMessage.success('已复制：' + text.slice(0, 30) + (text.length > 30 ? '...' : '')))
    .catch(() => ElMessage.info(text))
}

function copyTemplate(tpl: typeof templates[0]) {
  // 如果用户正在选中文字则不触发复制
  const selection = window.getSelection()
  if (selection && selection.toString().trim()) return
  const text = `模块标题：${tpl.sectionTitle}\n文案要点：${tpl.sectionCopy}\n画面描述：${tpl.visualDirection}\n比例：${tpl.aspectRatio}`
  navigator.clipboard.writeText(text).then(() => ElMessage.success('模板已复制，去商品编辑页粘贴即可'))
    .catch(() => ElMessage.info(text))
}

// ---- 比例 ----
const ratios = [
  { value: '3:4', label: '竖版 3:4', use: '详情页主图、小红书', bg: '#e8f0fe' },
  { value: '1:1', label: '方形 1:1', use: '商品主图、朋友圈', bg: '#e6f7e6' },
  { value: '16:9', label: '横版 16:9', use: '视频封面、Banner', bg: '#fef3e6' },
  { value: '9:16', label: '全屏 9:16', use: '手机海报、直播背景', bg: '#f5e6f7' },
]

// ---- 技巧 ----
const tips = [
  { title: '画面描述越具体越好', detail: '写清楚背景颜色、产品位置、光源方向、装饰元素。AI 不是读心术。' },
  { title: '文案要点控制在 3-4 条', detail: '太多文字会让图片拥挤，用 "|" 分隔，每条不超过 12 个字。' },
  { title: '模块标题 4-6 字最佳', detail: '"核心参数""产品亮点""材质细节"即可，太长反而破坏排版。' },
  { title: '风格和商品要匹配', detail: '科技产品 → 深色发光、护肤品 → 浅色自然、食品 → 暖色生活感。' },
  { title: '善用参考图', detail: '上传商品实拍图作为参考，能大幅提高生成图与实物的匹配度。' },
  { title: '先出图再微调', detail: '第一版不满意很正常，在画面描述里加细节再生成，比从零重写效果好。' },
]
</script>

<style scoped>
.guide-page { padding: 8px; max-width: 1300px; }

/* ---- Hero ---- */
.guide-hero {
  position: relative; overflow: hidden;
  border-radius: var(--radius-xl);
  padding: 40px 48px; margin-bottom: 32px;
  background: linear-gradient(135deg, #1a2332 0%, #15202b 40%, #1a2a38 100%);
  border: 1px solid var(--border-subtle);
}
.guide-hero .hero-glow {
  position: absolute; top: -50%; left: -20%; width: 80%; height: 200%;
  background: radial-gradient(ellipse, rgba(212,168,83,0.08) 0%, transparent 60%);
  pointer-events: none;
}
.guide-hero .hero-inner { position: relative; z-index: 1; }
.guide-hero h2 {
  font-family: var(--font-display); font-size: 28px; font-weight: 700;
  color: var(--text-primary); margin: 0 0 8px;
}
.guide-hero p { color: var(--text-secondary); font-size: 15px; margin: 0; }

/* ---- Sections ---- */
.guide-section { margin-bottom: 36px; }
.section-heading {
  font-size: 20px; font-weight: 700; color: var(--text-primary);
  margin: 0 0 8px; font-family: var(--font-display);
  display: flex; align-items: center; gap: 8px;
}
.section-icon { font-size: 22px; }
.count-badge {
  font-size: 12px; font-weight: 500; color: var(--accent-teal-light);
  background: rgba(91,138,138,0.1); padding: 2px 10px; border-radius: 12px;
  font-family: var(--font-body);
}
.section-sub { color: var(--text-muted); font-size: 13px; margin: 0 0 16px; }

/* ---- Field cards ---- */
.field-cards {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;
}
.field-card {
  text-align: center; padding: 24px 16px;
  background: var(--bg-surface); border-radius: var(--radius-lg);
  border: 1px solid var(--border-subtle);
  transition: all 0.3s var(--ease-out);
}
.field-card:hover { border-color: var(--accent-teal); transform: translateY(-2px); }
.field-icon { font-size: 40px; margin-bottom: 10px; }
.field-card h4 { margin: 0 0 6px; font-size: 16px; color: var(--text-primary); }
.field-desc { font-size: 13px; color: var(--text-secondary); margin: 0 0 8px; }
.field-example {
  font-size: 12px; color: var(--accent-light);
  background: rgba(212,168,83,0.06); padding: 6px 10px; border-radius: 8px; margin: 0;
}
@media (max-width: 768px) { .field-cards { grid-template-columns: 1fr; } }

/* ---- Template grid ---- */
.tpl-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px;
}
.tpl-card {
  border-radius: var(--radius-lg); overflow: hidden;
  border: 1px solid var(--border-subtle);
  background: var(--bg-surface);
  cursor: pointer;
  transition: all 0.35s var(--ease-out);
}
.tpl-card:hover {
  border-color: var(--accent-teal);
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0,0,0,0.4), 0 0 0 1px rgba(91,138,138,0.15);
}
.tpl-visual {
  padding: 30px 24px; text-align: center; position: relative;
  min-height: 100px; display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.tpl-emoji { font-size: 44px; display: block; margin-bottom: 4px; }
.tpl-name { font-size: 20px; font-weight: 700; color: #fff; text-shadow: 0 2px 8px rgba(0,0,0,0.3); }
.tpl-info { padding: 14px 16px; }
.tpl-field {
  display: flex; gap: 6px; margin-bottom: 6px; font-size: 12px; align-items: flex-start;
  cursor: pointer; padding: 4px 6px; border-radius: 6px;
  transition: background 0.2s var(--ease-out);
  user-select: text;
}
.tpl-field:hover { background: rgba(91,138,138,0.06); }
.tpl-field label {
  color: var(--accent-teal); font-weight: 600; min-width: 36px; flex-shrink: 0;
  user-select: none;
}
.tpl-field span {
  color: var(--text-secondary); line-height: 1.5;
  flex: 1; word-break: break-all;
}
.tpl-copy-btn {
  flex-shrink: 0; border: none; background: transparent;
  font-size: 14px; cursor: pointer; opacity: 0; padding: 0 2px;
  transition: opacity 0.2s var(--ease-out);
  user-select: none;
}
.tpl-field:hover .tpl-copy-btn { opacity: 0.7; }
.tpl-copy-btn:hover { opacity: 1 !important; }
.tpl-meta {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 8px; padding-top: 8px; border-top: 1px solid var(--border-subtle);
  user-select: none;
}
.tpl-copy-hint { font-size: 11px; color: var(--accent-teal); }

/* ---- Background grid ---- */
.bg-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 10px;
  margin-bottom: 16px;
}
.bg-card {
  border-radius: var(--radius-md); overflow: hidden;
  border: 2px solid var(--border-subtle);
  cursor: pointer; transition: all 0.3s var(--ease-out);
  background: var(--bg-surface);
}
.bg-card:hover { border-color: var(--border-default); transform: translateY(-2px); }
.bg-card.bg-selected {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(212,168,83,0.2);
}
.bg-preview { height: 80px; width: 100%; }
.bg-label {
  padding: 6px 8px; font-size: 11px; color: var(--text-secondary);
  text-align: center; white-space: nowrap;
}
.bg-check {
  text-align: center; font-size: 10px; color: var(--accent);
  padding: 2px 8px 6px; font-weight: 600;
}
.bg-prompt-box {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  padding: 12px 18px; background: var(--bg-surface); border-radius: var(--radius-md);
  border: 1px solid rgba(212,168,83,0.15);
}
.bg-prompt-label { font-size: 13px; color: var(--text-secondary); }
.bg-prompt-box code {
  font-size: 13px; color: var(--accent-light); background: rgba(212,168,83,0.06);
  padding: 4px 10px; border-radius: 6px; font-family: var(--font-mono);
}

/* ---- Ratio ---- */
.ratio-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.ratio-card { text-align: center; }
.ratio-box {
  border-radius: 12px; display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 700; color: var(--text-secondary);
  margin: 0 auto 8px; width: 100%; max-width: 120px; height: 80px;
}
.ratio-label { font-weight: 600; font-size: 14px; color: var(--text-primary); margin: 0 0 4px; }
.ratio-use { font-size: 12px; color: var(--text-muted); margin: 0; }
@media (max-width: 768px) { .ratio-row { grid-template-columns: repeat(2, 1fr); } }

/* ---- Tips ---- */
.tips-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 14px; }
.tip-card {
  display: flex; gap: 14px; align-items: flex-start;
  padding: 18px; background: var(--bg-surface); border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
  transition: all 0.3s var(--ease-out);
}
.tip-card:hover { border-color: var(--accent-teal); }
.tip-num {
  width: 32px; height: 32px; border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-dark), var(--accent));
  color: #0a0c12; display: flex; align-items: center; justify-content: center;
  font-size: 15px; font-weight: 700; flex-shrink: 0;
}
.tip-body strong { font-size: 14px; display: block; margin-bottom: 4px; color: var(--text-primary); }
.tip-body p { font-size: 13px; color: var(--text-secondary); margin: 0; line-height: 1.5; }
</style>

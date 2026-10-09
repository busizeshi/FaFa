(() => {
  const root = document.getElementById('app');
  const theme = document.body.className.replace('theme-', '');
  const state = { page: 'home', media: '全部', query: '', checked: new Set(['morning']), messages: [] };
  const pics = ['../assets/cat1.png', '../assets/cat-demo1.png', '../assets/cat2.png', '../assets/cat3.png'];
  const icon = (name, className = '') => `<i data-lucide="${name}" class="${className}" aria-hidden="true"></i>`;
  const esc = value => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const names = { sunny: '晴日陪伴', aurora: '柔光气泡', playful: '跳跳日记' };
  const topbar = () => `<header class="topbar"><div class="brand"><span class="brand-mark">${icon('paw-print')}</span> FaFa</div><div class="top-actions"><button class="icon-btn" data-toast="消息中心正在设计中" aria-label="消息">${icon('bell')}</button><button class="icon-btn" data-go="health" aria-label="查看宠物健康">${icon('cat')}</button></div></header>`;
  const heading = (title, sub, action, target) => `<div class="section-heading"><div><h2>${title}</h2>${sub ? `<p>${sub}</p>` : ''}</div>${action ? `<button class="text-link" data-go="${target}">${action} ${icon('arrow-up-right')}</button>` : ''}</div>`;
  function home() {
    return `${topbar()}
      <div class="intro-block"><div class="eyebrow">10 月 7 日 · 星期三</div><h1>早上好，<span class="soft-word">小鱼</span></h1><p>和团子一起，把今天过得有滋有味。</p></div>
      <section class="hero" aria-label="团子的今日卡片"><div class="hero-copy"><span class="hero-kicker">${icon('sparkles')} 今日状态不错</span><h2>团子的<br>小日子</h2><p>吃得香，玩得欢。<br>今天也被好好爱着。</p><button class="primary-btn" data-go="health">看看健康档案 ${icon('arrow-right')}</button></div><img class="hero-image" src="${pics[0]}" alt="银色虎斑猫团子"></section>
      <div class="metric-row"><div class="metric">${icon('heart-pulse')}<b>状态良好</b><span>今日健康</span></div><div class="metric">${icon('scale')}<b>4.2 kg</b><span>最近体重</span></div><div class="metric">${icon('camera')}<b>128 张</b><span>美好瞬间</span></div></div>
      ${heading('今天想做什么', '记录日常，也收藏快乐')}
      <div class="quick-grid"><button class="quick" data-go="health"><span class="quick-icon">${icon('heart-pulse')}</span>健康打卡</button><button class="quick" data-toast="喂养计划演示入口"><span class="quick-icon">${icon('utensils')}</span>喂养计划</button><button class="quick" data-go="media"><span class="quick-icon">${icon('images')}</span>记录瞬间</button><button class="quick" data-go="ai"><span class="quick-icon">${icon('sparkles')}</span>问问发发</button></div>
      ${heading('贴心提醒', '不遗漏团子的每件小事', '查看健康', 'health')}
      <button class="surface reminder" data-go="health" style="width:100%;text-align:left"><span class="reminder-symbol">${icon('calendar-heart')}</span><span class="reminder-copy"><strong>周五该做体内驱虫啦</strong><span>10 月 9 日 · 提前为团子准备</span></span>${icon('chevron-right','reminder-arrow')}</button>`;
  }
  function health() {
    const rows = [
      ['morning', '日常状态', '食欲、精神与排便', 'activity'],
      ['food', '喂养记录', '记录今天的餐食', 'utensils'],
      ['weight', '体重更新', '上次记录 10 月 2 日', 'scale']
    ];
    return `${topbar()}<div class="page-head"><div class="eyebrow">CARE & WELLNESS</div><h1>健康陪伴</h1><p>每一点日常观察，都是爱它的证据。</p></div>
      <button class="pet-switch" data-toast="当前演示宠物：团子"><img src="${pics[3]}" alt="团子头像">团子的健康档案 ${icon('chevron-down')}</button>
      <section class="health-hero"><span class="decor"></span><div class="eyebrow">团子的健康晴雨表</div><h2>今天状态<br>看起来不错</h2><p>最近 7 天记录比较稳定，继续保持规律照护。</p><img src="${pics[3]}" alt="团子近照"></section>
      <div class="health-stats"><div class="surface health-stat"><small>最近体重</small><strong>4.2 <span>kg</span></strong><span>10 月 2 日记录</span></div><div class="surface health-stat"><small>本周记录</small><strong>5 <span>天</span></strong><span>连续关注中</span></div></div>
      ${heading('今日照护清单', '轻轻一点，记录它的每一天')}
      <div class="surface checklist">${rows.map(([id,title,sub,ic]) => `<button class="check-row ${state.checked.has(id) ? 'done' : ''}" data-check="${id}" style="width:100%;border-left:0;border-right:0;border-top:0;background:none;text-align:left"><span class="check-icon">${icon(ic)}</span><span><strong>${title}</strong><br><small>${sub}</small></span><span class="check-dot">${state.checked.has(id) ? icon('check') : ''}</span></button>`).join('')}</div>
      ${heading('即将到来', '为重要时刻留个提醒')}
      <div class="surface reminder"><span class="reminder-symbol">${icon('shield-check')}</span><span class="reminder-copy"><strong>体内驱虫</strong><span>10 月 9 日 · 后天</span></span><span style="font-size:10px;color:var(--warm);font-weight:800">待完成</span></div>
      <p class="notice" style="margin-top:14px">${icon('info')}健康状态是基于日常记录的展示，不能替代兽医诊断。</p>`;
  }
  const media = [
    { title:'午后赖床', date:'10 月 6 日', type:'照片', src:pics[0] },
    { title:'逗猫棒挑战', date:'10 月 5 日', type:'视频', src:pics[1] },
    { title:'窗边散步', date:'10 月 3 日', type:'照片', src:pics[2] },
    { title:'团子的证件照', date:'9 月 29 日', type:'照片', src:pics[3] }
  ];
  function mediaPage() {
    const shown = media.filter(item => (state.media === '全部' || (state.media === '照片' && item.type === '照片') || (state.media === '视频' && item.type === '视频')) && (!state.query || item.title.includes(state.query)));
    return `${topbar()}<div class="page-head"><div class="eyebrow">LITTLE MOMENTS</div><h1>团子的影像馆</h1><p>把每个值得珍藏的瞬间，留在这里。</p></div>
      <label class="search-box">${icon('search')}<input id="media-search" type="search" placeholder="试试搜索：逗猫棒、窗边…" value="${esc(state.query)}" aria-label="搜索素材"><button type="button" data-toast="AI 语义搜索演示：输入标题可筛选下方素材">AI 搜索</button></label>
      <div class="chips" role="group" aria-label="素材分类">${['全部','照片','视频'].map(t => `<button class="chip ${state.media===t?'active':''}" data-media="${t}">${t}</button>`).join('')}<button class="chip" data-toast="按宠物分类演示入口">团子</button></div>
      <div class="media-lead"><h2>最近的快乐</h2><span>${shown.length} 个瞬间</span></div><div class="media-grid">${shown.length ? shown.map(item => `<button class="media-tile" data-toast="${item.title} · ${item.date}"><img src="${item.src}" alt="${item.title}"><span class="tile-shade"></span>${item.type==='视频'?`<span class="play-badge">${icon('play')}</span>`:''}<span class="tile-info">${item.title}<small>${item.date} · ${item.type}</small></span></button>`).join('') : `<p style="grid-column:1/-1;font-size:12px;color:var(--muted)">没有找到匹配的素材，换个词试试。</p>`}</div>
      <button class="upload-fab" data-toast="演示稿：上传请接入现有照片/视频接口">${icon('plus')} 添加瞬间</button>`;
  }
  function assistant() {
    const messages = state.messages.map(m => `<div class="bubble user">${esc(m)}</div><div class="bubble assistant"><strong>发发</strong>我已收到你的问题。正式接入后会结合团子的记录与素材给出有来源的回答。<div class="source">演示回复 · 暂未连接 AI 服务</div></div>`).join('');
    return `${topbar()}<div class="ai-title"><span class="ai-orb">${icon('sparkles')}</span><span><h1>问问发发</h1><p>懂团子，也懂你的宠物生活。</p></span></div>
      <div class="ai-banner"><div class="eyebrow">YOUR PET COMPANION</div><h2>关于团子的大小事，<br>我们一起想办法。</h2><p>健康记录、影像回忆、喂养问题，都可以从这里开始。</p></div>
      <div class="suggestions"><button data-prompt="团子最近体重怎么样？">${icon('scale')} 体重趋势</button><button data-prompt="帮我回顾团子这周的记录">${icon('notebook-tabs')} 本周回顾</button><button data-prompt="找找团子玩逗猫棒的视频">${icon('images')} 找视频</button></div>
      <div class="chat-label">10 月 7 日 · 与团子的对话</div><div class="bubble user">团子最近状态怎么样？</div><div class="bubble assistant"><strong>发发 ${icon('sparkles')}</strong>从最近的日常记录看，团子这周状态比较稳定。上次体重为 <b>4.2 kg</b>，你已经连续关注它 5 天啦。<div class="source">参考：体重记录 10/02 · 日常记录近 7 天</div></div>${messages}
      <form class="ai-composer" id="chat-form"><input name="message" placeholder="问问团子的健康、回忆或日常…" aria-label="发送给发发的消息" autocomplete="off"><button class="send-btn" aria-label="发送消息">${icon('arrow-up')}</button></form>`;
  }
  const nav = () => `<nav class="bottom-nav" aria-label="主要导航">${[
    ['home','house','首页'],['health','heart-pulse','健康'],['media','images','素材'],['ai','sparkles','发发']
  ].map(([id,ic,label]) => `<button class="nav-item ${state.page===id?'active':''}" data-go="${id}" ${state.page===id?'aria-current="page"':''}>${icon(ic)}<span>${label}</span></button>`).join('')}</nav>`;
  function render() {
    const content = ({ home, health, media: mediaPage, ai: assistant })[state.page]();
    root.innerHTML = `<div class="app-shell" data-theme="${theme}"><main class="screen" id="screen">${content}</main>${nav()}</div>`;
    if (window.lucide) window.lucide.createIcons();
  }
  let toastTimer;
  function toast(message) {
    root.querySelector('.toast')?.remove();
    const el = document.createElement('div'); el.className = 'toast'; el.role = 'status'; el.textContent = message;
    root.querySelector('.app-shell').appendChild(el);
    clearTimeout(toastTimer); toastTimer = setTimeout(() => el.remove(), 2300);
  }
  root.addEventListener('click', event => {
    const button = event.target.closest('button'); if (!button) return;
    if (button.dataset.go) { state.page = button.dataset.go; render(); return; }
    if (button.dataset.media) { state.media = button.dataset.media; render(); return; }
    if (button.dataset.check) { state.checked.has(button.dataset.check) ? state.checked.delete(button.dataset.check) : state.checked.add(button.dataset.check); render(); return; }
    if (button.dataset.prompt) { state.page = 'ai'; render(); root.querySelector('.ai-composer input').value = button.dataset.prompt; root.querySelector('.ai-composer input').focus(); return; }
    if (button.dataset.toast) toast(button.dataset.toast);
  });
  root.addEventListener('input', event => {
    if (event.target.id === 'media-search') {
      const pos = event.target.selectionStart;
      state.query = event.target.value.trim(); render();
      const input = root.querySelector('#media-search'); input.focus(); input.setSelectionRange(pos,pos);
    }
  });
  root.addEventListener('submit', event => {
    if (event.target.id !== 'chat-form') return; event.preventDefault();
    const value = event.target.elements.message.value.trim(); if (!value) return;
    state.messages.push(value); render(); root.querySelector('.screen').scrollTop = 10000;
  });
  document.title = `FaFa · ${names[theme] || '设计预览'}`;
  render();
})();

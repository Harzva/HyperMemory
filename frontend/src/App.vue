<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="brand-block">
        <span class="eyebrow">Knowledge QA Workbench</span>
        <h1>{{ productName }}</h1>
        <p>{{ productSubtitle }}</p>
      </div>

      <div class="header-controls">
        <div class="control mode-control" role="group" aria-label="Mode">
          <span>Mode</span>
          <div class="mode-toggle">
            <button
              v-for="mode in modes"
              :key="mode.value"
              type="button"
              :class="['mode-button', { active: selectedMode === mode.value }]"
              :aria-pressed="selectedMode === mode.value"
              @click="selectMode(mode.value)"
            >
              {{ mode.label }}
            </button>
          </div>
        </div>

        <label class="control">
          <span>Theme</span>
          <select v-model="selectedTheme">
            <option value="standard">Standard</option>
            <option value="forest">Forest</option>
            <option value="amber">Amber</option>
          </select>
        </label>
      </div>
    </header>

    <main class="workspace">
      <section class="panel upload-panel">
        <div class="panel-heading">
          <span class="step">01</span>
          <div>
            <h2>Upload Knowledge File</h2>
            <p>{{ currentMode.uploadHint }}</p>
          </div>
        </div>

        <div class="upload-row">
          <label class="file-input">
            <input type="file" @change="onFileChange" />
            <span>{{ selectedFile ? selectedFile.name : 'Choose file' }}</span>
          </label>
          <button class="primary-btn" @click="uploadFile" :disabled="!selectedFile || uploading">
            {{ uploading ? 'Uploading' : 'Upload' }}
          </button>
        </div>

        <p v-if="uploadStatus" class="status-text">{{ uploadStatus }}</p>
      </section>

      <section class="panel chat-panel">
        <div class="panel-heading">
          <span class="step">02</span>
          <div>
            <h2>Ask a Question</h2>
            <p>{{ currentMode.chatHint }}</p>
          </div>
          <span class="mode-pill">{{ currentMode.label }}</span>
        </div>

        <transition-group name="message" tag="div" class="messages" ref="messageContainer">
          <div v-if="messages.length === 0 && !loading" key="empty" class="empty-state">
            Start with a question about the uploaded knowledge base.
          </div>

          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['message', msg.role.toLowerCase()]"
          >
            <strong>{{ msg.role }}</strong>
            <span>{{ msg.content }}</span>
          </div>

          <div v-if="loading" key="loading" class="message assistant pending">
            <strong>Assistant</strong>
            <span>Thinking...</span>
          </div>
        </transition-group>

        <div class="input-row">
          <input
            v-model="userInput"
            @keyup.enter="sendQuestion"
            placeholder="Type your question..."
          />
          <button class="primary-btn" @click="sendQuestion" :disabled="!userInput.trim() || loading">
            Send
          </button>
        </div>
      </section>

      <section class="panel readiness-panel">
        <div class="panel-heading">
          <span class="step">03</span>
          <div>
            <h2>Production Readiness</h2>
            <p>{{ readinessSubtitle }}</p>
          </div>
        </div>

        <div class="signal-grid">
          <article v-for="signal in readinessSignals" :key="signal.label" class="signal-card">
            <span>{{ signal.label }}</span>
            <strong>{{ signal.value }}</strong>
            <small>{{ signal.detail }}</small>
          </article>
        </div>

        <div class="integration-strip">
          <span v-for="channel in botChannels" :key="channel" class="integration-pill">
            {{ channel }}
          </span>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';

const productName = 'HyperMemory';
const productSubtitle = 'Final memory-enhanced QA system with RAG, Agent, Wiki, GBrain, and Hyper modes.';

const modes = [
  {
    value: 'rag',
    label: 'RAG',
    uploadEndpoint: '/api/documents',
    chatEndpoint: '/api/chat',
    uploadHint: 'Index source documents for grounded retrieval.',
    chatHint: 'Answer directly from retrieved document chunks.',
  },
  {
    value: 'agent',
    label: 'Agent',
    uploadEndpoint: '/api/documents',
    chatEndpoint: '/api/agent/chat',
    uploadHint: 'Feed the shared retrieval index used by the agent.',
    chatHint: 'Let the agent call retrieval tools before answering.',
  },
  {
    value: 'wiki',
    label: 'LLM Wiki',
    uploadEndpoint: '/api/wiki/upload',
    chatEndpoint: '/api/wiki/chat',
    uploadHint: 'Convert uploaded knowledge into wiki-style memory.',
    chatHint: 'Read from the wiki memory generated from documents.',
  },
  {
    value: 'gbrain',
    label: 'GBrain',
    uploadEndpoint: '/api/wiki/upload',
    chatEndpoint: '/api/gbrain/chat',
    uploadHint: 'Prepare wiki memory for skill-oriented reasoning.',
    chatHint: 'Use GBrain skills over the wiki memory layer.',
  },
  {
    value: 'hyper',
    label: 'Hyper',
    uploadEndpoint: '/api/hyper/upload',
    chatEndpoint: '/api/hyper/chat',
    uploadHint: 'Ingest into the final HyperMemory layer.',
    chatHint: 'Use the final aggregation memory layer.',
  },
];

const themes = {
  standard: {
    '--surface': '#ffffff',
    '--surface-muted': '#f6f8fb',
    '--line': '#d9e0e8',
    '--text': '#17202a',
    '--muted': '#637083',
    '--primary': '#0f766e',
    '--primary-dark': '#115e59',
    '--primary-soft': '#d9f2ee',
    '--assistant': '#f3f6fb',
    '--accent': '#b45309',
  },
  forest: {
    '--surface': '#ffffff',
    '--surface-muted': '#f4f7f2',
    '--line': '#d7dfd2',
    '--text': '#18251c',
    '--muted': '#61705f',
    '--primary': '#287044',
    '--primary-dark': '#1f5735',
    '--primary-soft': '#ddf0e4',
    '--assistant': '#f4f7f2',
    '--accent': '#8a5a15',
  },
  amber: {
    '--surface': '#ffffff',
    '--surface-muted': '#f8f7f4',
    '--line': '#e3ded4',
    '--text': '#211f1a',
    '--muted': '#746d60',
    '--primary': '#8b5e10',
    '--primary-dark': '#6e490c',
    '--primary-soft': '#f4ead4',
    '--assistant': '#f8f7f4',
    '--accent': '#0f766e',
  },
};

const readinessSubtitle = 'Memory modes, Bot channels, observability, and deployment surfaces prepared for production review.';

const readinessSignals = [
  { label: 'Gateway', value: 'Feishu / DingTalk / WeChat', detail: 'Disabled until secrets are configured' },
  { label: 'Memory', value: 'RAG to Hyper', detail: 'Unified routing across all final modes' },
  { label: 'Metrics', value: '/actuator/prometheus', detail: 'Prometheus-ready runtime telemetry' },
  { label: 'Deploy', value: 'Compose + K8s', detail: 'Local stack and cluster template included' },
];

const botChannels = ['Feishu Bot', 'DingTalk Bot', 'WeChat Bot', 'RAG', 'Wiki', 'Agent', 'GBrain', 'Hyper'];

const selectedFile = ref(null);
const uploadStatus = ref('');
const uploading = ref(false);
const userInput = ref('');
const messages = ref([]);
const loading = ref(false);
const selectedMode = ref('hyper');
const selectedTheme = ref('standard');
const messageContainer = ref(null);
const apiToken = ref(import.meta.env.VITE_API_TOKEN ?? '');
let messageCounter = 0;

const currentMode = computed(() => modes.find((mode) => mode.value === selectedMode.value) ?? modes[0]);

function selectMode(mode) {
  selectedMode.value = mode;
}

function applyTheme() {
  const theme = themes[selectedTheme.value] ?? themes.standard;
  Object.entries(theme).forEach(([key, value]) => {
    document.documentElement.style.setProperty(key, value);
  });
}

onMounted(() => {
  applyRoutePreview();
  applyTheme();
});
watch(selectedTheme, applyTheme);

function applyRoutePreview() {
  const params = new URLSearchParams(window.location.search);
  const mode = params.get('mode');
  const theme = params.get('theme');
  const demo = params.get('demo');
  const token = params.get('apiToken');

  if (modes.some((candidate) => candidate.value === mode)) {
    selectedMode.value = mode;
  }
  if (theme && themes[theme]) {
    selectedTheme.value = theme;
  }
  if (token) {
    apiToken.value = token;
    window.localStorage?.setItem('campusQaApiToken', token);
  } else {
    apiToken.value = window.localStorage?.getItem('campusQaApiToken') || apiToken.value;
  }
  if (demo === 'conversation') {
    messages.value = [
      { id: ++messageCounter, role: 'User', content: 'Summarize the scholarship process and keep session memory.' },
      {
        id: ++messageCounter,
        role: 'Assistant',
        content: `${currentMode.value.label} combined retrieved policy context with memory state and returned a guarded answer.`,
      },
    ];
  }
  if (demo === 'upload') {
    uploadStatus.value = `Indexed scholarship-policy.md for ${currentMode.value.label}.`;
  }
}

function onFileChange(event) {
  selectedFile.value = event.target.files?.[0] ?? null;
  uploadStatus.value = '';
}

function scrollToBottom() {
  nextTick(() => {
    const container = messageContainer.value?.$el ?? messageContainer.value;
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  });
}

function apiHeaders(base = {}) {
  return apiToken.value ? { ...base, 'X-API-Token': apiToken.value } : base;
}

async function uploadFile() {
  if (!selectedFile.value || uploading.value) return;

  const formData = new FormData();
  formData.append('file', selectedFile.value);
  uploading.value = true;
  uploadStatus.value = 'Uploading and indexing...';

  try {
    const response = await fetch(currentMode.value.uploadEndpoint, {
      method: 'POST',
      headers: apiHeaders(),
      body: formData,
    });
    if (!response.ok) {
      throw new Error(response.statusText || `HTTP ${response.status}`);
    }
    uploadStatus.value = `Indexed for ${currentMode.value.label}.`;
    selectedFile.value = null;
  } catch (error) {
    uploadStatus.value = `Upload failed: ${error.message}`;
  } finally {
    uploading.value = false;
  }
}

async function sendQuestion() {
  const input = userInput.value.trim();
  if (!input || loading.value) return;

  messages.value.push({ id: ++messageCounter, role: 'User', content: input });
  userInput.value = '';
  loading.value = true;
  scrollToBottom();

  const assistantMessage = { id: ++messageCounter, role: 'Assistant', content: '' };

  try {
    const response = await fetch(currentMode.value.chatEndpoint, {
      method: 'POST',
      headers: apiHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ conversationId: 'default', userInput: input }),
    });
    if (!response.ok) {
      throw new Error(response.statusText || `HTTP ${response.status}`);
    }

    messages.value.push(assistantMessage);
    await readResponse(response, assistantMessage);
  } catch (error) {
    messages.value.push({
      id: assistantMessage.id,
      role: 'Assistant',
      content: `Error: ${error.message}`,
    });
  } finally {
    loading.value = false;
    scrollToBottom();
  }
}

async function readResponse(response, assistantMessage) {
  if (!response.body) {
    assistantMessage.content = await response.text();
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { value, done } = await reader.read();
    if (value) {
      assistantMessage.content += decoder.decode(value, { stream: true });
      scrollToBottom();
    }
    if (done) break;
  }
}
</script>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(body) {
  margin: 0;
  background: var(--surface-muted);
  color: var(--text);
  font-family: "Segoe UI", "Microsoft YaHei", system-ui, sans-serif;
  overflow-x: hidden;
}

.app-shell {
  min-height: 100vh;
  padding-bottom: 32px;
}

.app-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px clamp(18px, 5vw, 64px);
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}

.brand-block {
  max-width: 780px;
}

.eyebrow {
  color: var(--accent);
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
}

h1,
h2,
p {
  margin: 0;
}

.brand-block h1 {
  margin-top: 8px;
  font-size: clamp(2rem, 4vw, 3.7rem);
  line-height: 1;
}

.brand-block p {
  margin-top: 12px;
  color: var(--muted);
  font-size: 1rem;
  line-height: 1.55;
}

.header-controls {
  display: flex;
  align-items: end;
  gap: 12px;
  flex-wrap: wrap;
  min-width: 0;
}

.control {
  display: grid;
  gap: 6px;
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 700;
}

.mode-control {
  width: min(640px, 100%);
}

.mode-toggle {
  width: 100%;
  min-height: 42px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--surface);
}

.mode-button {
  min-height: 32px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--muted);
  font: inherit;
  font-size: 0.86rem;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.16s ease, color 0.16s ease, box-shadow 0.16s ease;
}

.mode-button:hover {
  color: var(--text);
  background: var(--surface-muted);
}

.mode-button.active {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 6px 14px rgba(15, 118, 110, 0.18);
}

select,
input {
  min-height: 42px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: var(--surface);
  color: var(--text);
  font: inherit;
}

select {
  min-width: 142px;
  padding: 0 12px;
}

.workspace {
  width: min(1080px, calc(100% - 32px));
  margin: 28px auto 0;
  display: grid;
  grid-template-columns: minmax(280px, 0.8fr) minmax(0, 1.6fr);
  gap: 18px;
  min-width: 0;
}

.panel {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 18px;
  box-shadow: 0 16px 34px rgba(23, 32, 42, 0.06);
  min-width: 0;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
}

.panel-heading h2 {
  font-size: 1.05rem;
}

.panel-heading p {
  margin-top: 4px;
  color: var(--muted);
  font-size: 0.9rem;
  line-height: 1.45;
}

.step,
.mode-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 38px;
  height: 28px;
  border-radius: 6px;
  background: var(--primary-soft);
  color: var(--primary-dark);
  font-size: 0.78rem;
  font-weight: 800;
}

.mode-pill {
  margin-left: auto;
  padding: 0 10px;
}

.upload-row,
.input-row {
  display: flex;
  gap: 10px;
}

.file-input {
  flex: 1;
  min-height: 46px;
  display: flex;
  align-items: center;
  padding: 0 12px;
  border: 1px dashed var(--line);
  border-radius: 6px;
  color: var(--muted);
  cursor: pointer;
}

.file-input input {
  display: none;
}

.file-input span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.primary-btn {
  min-height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: 6px;
  background: var(--primary);
  color: #fff;
  font-weight: 750;
  cursor: pointer;
}

.primary-btn:hover:not(:disabled) {
  background: var(--primary-dark);
}

.primary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.status-text {
  margin-top: 12px;
  color: var(--muted);
  font-size: 0.9rem;
}

.chat-panel {
  min-height: 560px;
  display: flex;
  flex-direction: column;
}

.readiness-panel {
  grid-column: 1 / -1;
}

.signal-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.signal-card {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface-muted);
}

.signal-card span {
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 800;
  text-transform: uppercase;
}

.signal-card strong {
  color: var(--text);
  font-size: 1rem;
  line-height: 1.25;
}

.signal-card small {
  color: var(--muted);
  line-height: 1.4;
}

.integration-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.integration-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 700;
}

.messages {
  flex: 1;
  min-height: 360px;
  max-height: 520px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  overflow-y: auto;
  background: var(--surface-muted);
  border: 1px solid var(--line);
  border-radius: 8px;
}

.empty-state {
  margin: auto;
  color: var(--muted);
  text-align: center;
  max-width: 100%;
  overflow-wrap: anywhere;
}

.message {
  max-width: min(76%, 720px);
  padding: 10px 12px;
  border-radius: 8px;
  line-height: 1.55;
  word-break: break-word;
}

.message strong {
  display: block;
  margin-bottom: 3px;
  font-size: 0.78rem;
}

.message span {
  white-space: pre-wrap;
}

.message.user {
  align-self: flex-end;
  background: var(--primary);
  color: #fff;
}

.message.assistant {
  align-self: flex-start;
  background: var(--assistant);
  border: 1px solid var(--line);
  color: var(--text);
}

.message.pending span::after {
  content: "";
  display: inline-block;
  width: 0.8em;
  animation: pulse 1.1s infinite;
}

.input-row {
  margin-top: 12px;
}

.input-row input {
  flex: 1;
  min-width: 0;
  padding: 0 12px;
}

.message-enter-active,
.message-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.message-enter-from,
.message-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.25;
  }
  50% {
    opacity: 1;
  }
}

@media (max-width: 820px) {
  .app-header,
  .workspace {
    grid-template-columns: 1fr;
  }

  .app-header {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    width: 100%;
  }

  .header-controls,
  .upload-row,
  .input-row {
    align-items: stretch;
    flex-direction: column;
  }

  .header-controls,
  .control {
    width: 100%;
  }

  .mode-control {
    width: calc(100vw - 48px);
    max-width: 100%;
    min-width: 0;
  }

  .mode-toggle {
    max-width: 100%;
  }

  .brand-block p {
    overflow-wrap: anywhere;
  }

  .brand-block,
  .messages {
    min-width: 0;
    max-width: 100%;
  }

  select,
  .mode-toggle,
  input,
  .file-input,
  .primary-btn {
    width: 100%;
    min-width: 0;
  }

  select {
    width: 100%;
  }

  .mode-toggle {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chat-panel {
    min-height: 480px;
  }

  .message {
    max-width: 92%;
  }

  .signal-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div ref="root" class="app-select" :class="{ open: opened }">
    <button
      class="app-select-trigger"
      type="button"
      :aria-expanded="opened"
      @click="toggle"
      @keydown.down.prevent="move(1)"
      @keydown.up.prevent="move(-1)"
      @keydown.enter.prevent="choose(activeOption)"
      @keydown.esc.prevent="close"
    >
      <span :class="{ placeholder: selectedLabel === placeholder }">{{ selectedLabel }}</span>
      <span class="app-select-arrow">⌄</span>
    </button>

    <Transition name="select-pop">
      <div v-if="opened" class="app-select-menu">
        <button
          v-for="(option, index) in normalizedOptions"
          :key="`${option.value}-${index}`"
          class="app-select-option"
          :class="{ selected: isSelected(option), active: index === activeIndex }"
          type="button"
          @mouseenter="activeIndex = index"
          @click="choose(option)"
        >
          <span>{{ option.label }}</span>
          <span v-if="isSelected(option)" class="app-select-check">✓</span>
        </button>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps({
  modelValue: {
    type: [String, Number, Boolean, null],
    default: ''
  },
  options: {
    type: Array,
    required: true
  },
  placeholder: {
    type: String,
    default: '请选择'
  }
});

const emit = defineEmits(['update:modelValue', 'change']);

const root = ref(null);
const opened = ref(false);
const activeIndex = ref(0);

const normalizedOptions = computed(() => props.options.map((option) => {
  if (typeof option === 'object') {
    return option;
  }
  return { label: String(option), value: option };
}));

const selectedOption = computed(() => normalizedOptions.value.find((option) => option.value === props.modelValue));
const selectedLabel = computed(() => selectedOption.value?.label ?? props.placeholder);
const activeOption = computed(() => normalizedOptions.value[activeIndex.value]);

function toggle() {
  opened.value ? close() : open();
}

function open() {
  opened.value = true;
  const selectedIndex = normalizedOptions.value.findIndex((option) => option.value === props.modelValue);
  activeIndex.value = selectedIndex >= 0 ? selectedIndex : 0;
}

function close() {
  opened.value = false;
}

function choose(option) {
  if (!option) return;
  emit('update:modelValue', option.value);
  emit('change', option.value);
  close();
}

function isSelected(option) {
  return option.value === props.modelValue;
}

function move(step) {
  if (!opened.value) {
    open();
    return;
  }
  const length = normalizedOptions.value.length;
  activeIndex.value = (activeIndex.value + step + length) % length;
}

function handleDocumentClick(event) {
  if (!root.value?.contains(event.target)) {
    close();
  }
}

onMounted(() => document.addEventListener('click', handleDocumentClick));
onBeforeUnmount(() => document.removeEventListener('click', handleDocumentClick));
</script>

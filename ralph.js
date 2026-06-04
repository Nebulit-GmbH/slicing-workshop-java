#!/usr/bin/env node
// Ralph - Long-running AI agent loop for Node.js slice-based development
// Usage: node ralph.js [max_iterations]

import { spawn } from 'child_process';
import { createWriteStream, existsSync, mkdirSync, readFileSync, writeFileSync, appendFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const MAX_ITERATIONS = parseInt(process.argv[2] || '10', 10);
const PROGRESS_FILE = join(__dirname, 'progress.txt');
const ARCHIVE_DIR = join(__dirname, 'archive');

mkdirSync(ARCHIVE_DIR, { recursive: true });

if (!existsSync(PROGRESS_FILE)) {
  writeFileSync(PROGRESS_FILE, `# Event Model Development Progress Log\nStarted: ${new Date()}\n---\n`);
}

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

function runClaude(promptFile, progressFile) {
  return new Promise((resolve) => {
    const promptContent = readFileSync(promptFile, 'utf8');
    const claude = spawn('claude', ['--dangerously-skip-permissions'], {
      stdio: ['pipe', 'pipe', 'pipe'],
    });

    claude.stdin.write(promptContent);
    claude.stdin.end();

    let output = '';
    const progressStream = createWriteStream(progressFile, { flags: 'a' });

    const handleData = (data) => {
      const text = data.toString();
      output += text;
      process.stdout.write(text);
      progressStream.write(text);
    };

    claude.stdout.on('data', handleData);
    claude.stderr.on('data', handleData);

    claude.on('close', (exitCode) => {
      progressStream.end(() => resolve({ exitCode, output }));
    });
  });
}

async function main() {
  console.log(`Starting Ralph – Max iterations: ${MAX_ITERATIONS}`);

  for (let i = 1; i <= MAX_ITERATIONS; i++) {
    console.log();
    console.log('═══════════════════════════════════════════════════════');
    console.log(`  Ralph Iteration ${i} of ${MAX_ITERATIONS}`);
    console.log('═══════════════════════════════════════════════════════');
    console.log();
    console.log(`>>> Running Claude at ${new Date()}`);
    appendFileSync(PROGRESS_FILE, `>>> Iteration ${i}\n`);

    let claudeSkip = false;
    let output = '';

    while (true) {
      const result = await runClaude(join(__dirname, 'prompt.md'), PROGRESS_FILE);
      output = result.output;

      if (result.exitCode === 0) {
        break;
      } else if (output.includes('No messages returned')) {
        console.log();
        console.log('⚠️ Claude returned no messages (transient). Skipping iteration...');
        claudeSkip = true;
        break;
      } else {
        console.log();
        console.log('⚠️ Claude exited with an error. Possibly spending limit reached.');
        console.log('Waiting 5 minutes before retry...');
        await sleep(300_000);
      }
    }

    if (claudeSkip) continue;

    if (!output.trim()) {
      console.log('⚠️ Claude returned no output. Retrying in 1 minute...');
      await sleep(60_000);
      continue;
    }

    if (output.includes('❌ COMMIT REVIEW FAILED')) {
      console.log();
      console.log('⚠️  Commit review failed - workspace has been reset');
      console.log('📄 Review failure details recorded in progress.txt');
      console.log('🔄 Continuing to next iteration...');
      console.log();
      appendFileSync(PROGRESS_FILE, `Review failed at iteration ${i} - continuing loop\nContinuing: ${new Date()}\n---\n`);
      await sleep(5_000);
      continue;
    }

    if (output.includes('<promise>COMPLETE</promise>')) {
      console.log();
      console.log('🎉 Ralph completed all tasks!');
      console.log(`Completed at iteration ${i} of ${MAX_ITERATIONS}`);
      console.log();
      appendFileSync(PROGRESS_FILE, `Completed: ${new Date()}\n`);
      process.exit(0);
    }

    if (output.includes('<promise>NO_TASKS</promise>')) {
      console.log();
      console.log('⏳ No tasks available. Waiting 30 seconds before next check...');
      await sleep(30_000);
      continue;
    }

    console.log();
    console.log(`Iteration ${i} complete. Continuing...`);
    await sleep(2_000);
  }

  console.log();
  console.log(`⚠️ Ralph reached max iterations (${MAX_ITERATIONS}) without completing all tasks.`);
  console.log(`Check ${PROGRESS_FILE} for status.`);
  process.exit(1);
}

main();
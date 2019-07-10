import fs = require('fs');
export class StatusCommand {
  handler(program: any) {
    return () => {
      const moduleDir: string = program.opts().moduleDir;
      const workspaceDir = moduleDir.substring(0, moduleDir.lastIndexOf('/'));
      console.log(`status: ${workspaceDir}`);
      const dir = fs.readdirSync(workspaceDir);
      console.dir(dir);
    };
  }
}

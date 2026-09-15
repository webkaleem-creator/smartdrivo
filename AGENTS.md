# Project Instructions

## Post-Build Automation
After every successful build (`compile_applet`), automatically run:
```bash
git add .
git commit -m "auto: SmartDrivo update"
git push origin main
```

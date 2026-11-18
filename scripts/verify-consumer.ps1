$ErrorActionPreference = 'Stop'
& python (Join-Path $PSScriptRoot 'verify-consumer.py')
exit $LASTEXITCODE

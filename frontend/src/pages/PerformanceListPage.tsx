import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardActionArea from '@mui/material/CardActionArea'
import CardContent from '@mui/material/CardContent'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { getPerformances } from '../api/performances'
import type { Performance } from '../api/performances'
import { StatusBadge } from '../components/StatusBadge'
import { fontDisplay } from '../theme/tokens'

type ListState =
  | { status: 'loading' }
  | { status: 'error' }
  | { status: 'success'; performances: Performance[] }

const SKELETON_COUNT = 4

export function PerformanceListPage() {
  const [state, setState] = useState<ListState>({ status: 'loading' })

  const load = useCallback(() => {
    setState({ status: 'loading' })
    getPerformances()
      .then((response) => setState({ status: 'success', performances: response.content }))
      .catch(() => setState({ status: 'error' }))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  if (state.status === 'loading') {
    return (
      <Stack spacing={2}>
        {Array.from({ length: SKELETON_COUNT }).map((_, index) => (
          <Skeleton
            key={index}
            data-testid="performance-card-skeleton"
            variant="rectangular"
            height={96}
          />
        ))}
      </Stack>
    )
  }

  if (state.status === 'error') {
    return (
      <Alert
        severity="error"
        action={
          <Button color="inherit" size="small" onClick={load}>
            다시 시도
          </Button>
        }
      >
        목록을 불러오지 못했습니다
      </Alert>
    )
  }

  if (state.performances.length === 0) {
    return <Typography>예정된 공연이 없습니다</Typography>
  }

  return (
    <Stack spacing={2}>
      {state.performances.map((performance) => (
        <Card key={performance.id}>
          <CardActionArea component={Link} to={`/performances/${performance.id}`}>
            <CardContent>
              <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
                <Box>
                  <Typography variant="h6" sx={{ fontFamily: fontDisplay }}>
                    {performance.title}
                  </Typography>
                  <Typography variant="body2" color="textSecondary">
                    {performance.venue} · {new Date(performance.startAt).toLocaleString()}
                  </Typography>
                </Box>
                <StatusBadge status={performance.status} />
              </Stack>
            </CardContent>
          </CardActionArea>
        </Card>
      ))}
    </Stack>
  )
}

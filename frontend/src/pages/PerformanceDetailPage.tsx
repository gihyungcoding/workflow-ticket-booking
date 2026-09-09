import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Button from '@mui/material/Button'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { getPerformance, PerformanceNotFoundError } from '../api/performances'
import type { Performance } from '../api/performances'
import { StatusBadge } from '../components/StatusBadge'

type DetailState =
  | { status: 'loading' }
  | { status: 'not-found' }
  | { status: 'error' }
  | { status: 'success'; performance: Performance }

export function PerformanceDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [state, setState] = useState<DetailState>({ status: 'loading' })

  useEffect(() => {
    if (!id) return
    setState({ status: 'loading' })
    getPerformance(id)
      .then((performance) => setState({ status: 'success', performance }))
      .catch((error: unknown) => {
        if (error instanceof PerformanceNotFoundError) {
          setState({ status: 'not-found' })
        } else {
          setState({ status: 'error' })
        }
      })
  }, [id])

  if (state.status === 'loading') {
    return <Skeleton variant="rectangular" height={240} />
  }

  if (state.status === 'not-found') {
    return (
      <Stack spacing={2} sx={{ alignItems: 'flex-start' }}>
        <Typography>존재하지 않는 공연입니다</Typography>
        <Button component={Link} to="/" variant="outlined">
          목록으로
        </Button>
      </Stack>
    )
  }

  if (state.status === 'error') {
    return <Typography>상세 정보를 불러오지 못했습니다</Typography>
  }

  const { performance } = state
  return (
    <Stack spacing={1}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h5">{performance.title}</Typography>
        <StatusBadge status={performance.status} />
      </Stack>
      <Typography>{performance.venue}</Typography>
      <Typography>공연 일시: {new Date(performance.startAt).toLocaleString()}</Typography>
      <Typography>예매 오픈: {new Date(performance.openAt).toLocaleString()}</Typography>
      <Typography>예매 마감: {new Date(performance.closeAt).toLocaleString()}</Typography>
      <Typography>잔여 좌석: {performance.availableSeats}석</Typography>
    </Stack>
  )
}
